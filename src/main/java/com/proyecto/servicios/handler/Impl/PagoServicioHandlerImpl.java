package com.proyecto.servicios.handler.Impl;

import com.proyecto.servicios.dto.ConfirmacionTxResponseDTO;
import com.proyecto.servicios.dto.Solicitud2FARequestDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Saldo;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import com.proyecto.servicios.exception.CuentaNoAutorizadaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ProductoNoValidoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.handler.PagoServicioHandler;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.SaldoRepository;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.service.SegundoFactorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Implementación del patrón Handler para orquestar el flujo de pagos GestoPago.
 * Separa la capa Controller de la capa Service y coordina validaciones y llamadas externas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PagoServicioHandlerImpl implements PagoServicioHandler {

    private final PagoServicioService pagoServicioService;
    private final SegundoFactorService segundoFactorService;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GestoPagoProductoRepository productoRepository;
    private final SaldoRepository saldoRepository;

    @Override
    public GenericResponse solicitar2FA(Solicitud2FARequestDTO request, String usernameAutenticado) {
        log.info("Orquestando solicitud de 2FA para usuario={}, cuenta={}, servicio={}, monto={}",
                usernameAutenticado, request.getNumCuenta(), request.getIdServicio(), request.getMonto());

        // 1. Validar existencia de cuenta
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta indicada no existe."));

        // 2. Validar propiedad y autorización del usuario
        Usuario usuario = usuarioRepository.findByUsername(usernameAutenticado)
                .orElseThrow(() -> new CuentaNoAutorizadaException("No está autorizado para operar esta cuenta."));

        if (!Boolean.TRUE.equals(usuario.getActivo()) || Boolean.TRUE.equals(usuario.getBloqueado())) {
            throw new CuentaNoAutorizadaException("El usuario no está habilitado para realizar operaciones.");
        }

        Cliente cliente = cuenta.getCliente();
        if (cliente == null || usuario.getCliente() == null
                || !cliente.getId().equals(usuario.getCliente().getId())) {
            throw new CuentaNoAutorizadaException("No está autorizado para operar esta cuenta.");
        }

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new CuentaNoAutorizadaException("El cliente no está activo.");
        }

        // 3. Validar estatus activo de la cuenta
        if (cuenta.getEstatusCuenta() == null
                || !EstadoCuenta.ACTIVA.name().equalsIgnoreCase(cuenta.getEstatusCuenta().getNombre())
                || !Boolean.TRUE.equals(cuenta.getEstatusCuenta().getActivo())) {
            throw new CuentaNoAutorizadaException("La cuenta no se encuentra activa.");
        }

        // 4. Validar producto y monto contra catálogo de GestoPago
        GestoPagoProducto producto = productoRepository
                .findFirstByIdServicioAndIdProducto(request.getIdServicio(), request.getIdProducto())
                .orElseThrow(() -> productoRepository.existsByIdServicio(request.getIdServicio())
                        ? new ProductoNoValidoException("El producto no corresponde al servicio indicado.")
                        : new ProductoNoValidoException("El servicio indicado no existe."));

        if (producto.getPrecio() != null && producto.getPrecio() > 0) {
            BigDecimal precioFijo = BigDecimal.valueOf(producto.getPrecio());
            if (precioFijo.compareTo(request.getMonto()) != 0) {
                throw new ProductoNoValidoException("El monto no corresponde al precio del producto.");
            }
        }

        // 5. Validar saldo suficiente
        Saldo saldo = cuenta.getSaldo();
        if (saldo == null) {
            saldo = saldoRepository.findByCuentaId(cuenta.getId()).orElse(null);
        }
        if (saldo == null || saldo.getSaldoDisponible() == null
                || saldo.getSaldoDisponible().compareTo(request.getMonto()) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para realizar el pago.");
        }

        // 6. Generar y enviar OTP vía Brevo
        String nombreCliente = cliente.getNombre() + (cliente.getApellidoPaterno() != null ? " " + cliente.getApellidoPaterno() : "");
        int vigenciaSegundos = segundoFactorService.solicitarOtp(
                cliente.getId(), cliente.getCorreo(), nombreCliente, cuenta.getId(),
                request.getIdServicio(), request.getIdProducto(), request.getMonto());

        return GenericResponse.builder()
                .codigo(0)
                .mensaje("Código de segunda autenticación enviado con éxito a su correo electrónico registrado.")
                .data(Map.of(
                        "vigenciaSegundos", vigenciaSegundos,
                        "correoEnmascarado", enmascararCorreo(cliente.getCorreo()),
                        "monto", request.getMonto()
                ))
                .build();
    }

    @Override
    public ResponseEntity<GenericResponse> procesarPago(PagoServicioRequest request, String usernameAutenticado) {
        log.info("Orquestando procesamiento de pago para usuario={}, cuenta={}, servicio={}, monto={}",
                usernameAutenticado, request.getNumCuenta(), request.getIdServicio(), request.getMonto());

        PagoServicioResponse response = pagoServicioService.pagarServicio(request, usernameAutenticado);

        // Mapeo riguroso de acuerdo a especificaciones:
        // Exitoso: Código interno '0', HTTP 200 OK
        // Pendiente / Timeout: Código interno '82', HTTP 202 ACCEPTED
        // Rechazado: Código interno '422', HTTP 422 UNPROCESSABLE_ENTITY
        if (response.getEstado() == EstadoPagoServicio.APLICADO) {
            GenericResponse body = GenericResponse.builder()
                    .codigo(0)
                    .mensaje(response.getMensaje() != null ? response.getMensaje() : "Pago aplicado exitosamente.")
                    .data(response)
                    .build();
            return new ResponseEntity<>(body, HttpStatus.OK);
        } else if (response.getEstado() == EstadoPagoServicio.PENDIENTE_CONFIRMACION) {
            GenericResponse body = GenericResponse.builder()
                    .codigo(82)
                    .mensaje(response.getMensaje() != null ? response.getMensaje() : "Operación pendiente de confirmación por el proveedor.")
                    .data(response)
                    .build();
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        } else {
            GenericResponse body = GenericResponse.builder()
                    .codigo(422)
                    .mensaje(response.getMensaje() != null ? response.getMensaje() : "El pago fue rechazado por el proveedor.")
                    .data(response)
                    .build();
            return new ResponseEntity<>(body, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @Override
    public GenericResponse conciliarTransaccion(String upc) {
        log.info("Orquestando conciliación de transacción upc={}", upc);
        PagoServicioResponse respuesta = pagoServicioService.conciliarTransaccion(upc);

        int codigoInterno = (respuesta.getEstado() == EstadoPagoServicio.APLICADO) ? 0 :
                (respuesta.getEstado() == EstadoPagoServicio.PENDIENTE_CONFIRMACION ? 82 : 422);

        return GenericResponse.builder()
                .codigo(codigoInterno)
                .mensaje("Resultado de conciliación: " + respuesta.getEstado().name())
                .data(respuesta)
                .build();
    }

    private String enmascararCorreo(String correo) {
        if (correo == null || !correo.contains("@")) return "***";
        int at = correo.indexOf('@');
        String u = correo.substring(0, at);
        String d = correo.substring(at);
        if (u.length() <= 2) return u.charAt(0) + "***" + d;
        return u.charAt(0) + "***" + u.charAt(u.length() - 1) + d;
    }
}
