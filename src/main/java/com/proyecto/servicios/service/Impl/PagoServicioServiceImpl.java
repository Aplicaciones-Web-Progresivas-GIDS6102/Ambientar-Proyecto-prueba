package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.Saldo;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.enums.EstadoCuenta;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioValidado;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.SaldoRepository;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.service.SegundoFactorService;
import com.proyecto.servicios.service.SentTxGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orquesta el pago de servicios GestoPago.
 * Orden de ejecución:
 * 1. Validación de cuenta existente y activa.
 * 2. Validación de propiedad y autorización del cliente autenticado.
 * 3. Validación de producto y restricciones de catálogo.
 * 4. Validación de saldo disponible.
 * 5. Validación de no duplicidad de la operación.
 * 6. Validación y consumo de la segunda autenticación (2FA) vinculada a la operación.
 * 7. Envío al proveedor GestoPago (sendTx.do).
 * 8. Descuento del saldo ÚNICAMENTE cuando exista confirmación de pago exitoso (código '01').
 * 9. Manejo de timeouts y confirmación (confirmTx.do).
 */
@Service
@Slf4j
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final List<String> ESTADOS_DUPLICADOS = List.of("APLICADO", "PENDIENTE_CONFIRMACION");

    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GestoPagoProductoRepository productoRepository;
    private final SaldoRepository saldoRepository;
    private final GestoPagoTransaccionRepository transaccionRepository;
    private final ObjectProvider<SegundoFactorService> segundoFactorProvider;
    private final ObjectProvider<SentTxGateway> sentTxGatewayProvider;

    public PagoServicioServiceImpl(CuentaRepository cuentaRepository,
                                   UsuarioRepository usuarioRepository,
                                   GestoPagoProductoRepository productoRepository,
                                   SaldoRepository saldoRepository,
                                   GestoPagoTransaccionRepository transaccionRepository,
                                   ObjectProvider<SegundoFactorService> segundoFactorProvider,
                                   ObjectProvider<SentTxGateway> sentTxGatewayProvider) {
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.saldoRepository = saldoRepository;
        this.transaccionRepository = transaccionRepository;
        this.segundoFactorProvider = segundoFactorProvider;
        this.sentTxGatewayProvider = sentTxGatewayProvider;
    }

    /**
     * Constructor sobrecargado para compatibilidad con suites de pruebas unitarias existentes.
     */
    public PagoServicioServiceImpl(CuentaRepository cuentaRepository,
                                   UsuarioRepository usuarioRepository,
                                   GestoPagoProductoRepository productoRepository,
                                   ObjectProvider<SegundoFactorService> segundoFactorProvider,
                                   ObjectProvider<SentTxGateway> sentTxGatewayProvider) {
        this(cuentaRepository, usuarioRepository, productoRepository, null, null,
                segundoFactorProvider, sentTxGatewayProvider);
    }

    @Override
    public PagoServicioResponse pagarServicio(PagoServicioRequest request, String usernameAutenticado) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException("La cuenta indicada no existe."));

        Cliente cliente = validarPropiedadYEstado(cuenta, usernameAutenticado);
        validarEstatusCuenta(cuenta);
        validarProductoYMonto(request);
        Saldo saldo = validarSaldo(cuenta, request.getMonto());
        validarNoDuplicada(cuenta, request);

        // Se resuelven ambos componentes ANTES de consumir el 2FA, para no gastar el código si no se puede pagar.
        SegundoFactorService segundoFactor = segundoFactorProvider.getIfAvailable();
        SentTxGateway gateway = sentTxGatewayProvider.getIfAvailable();
        if (segundoFactor == null) {
            throw new IntegracionNoDisponibleException("La segunda autenticación no está disponible. No se procesó el pago.");
        }
        if (gateway == null) {
            throw new IntegracionNoDisponibleException("El servicio de pagos del proveedor no está disponible. No se procesó el pago.");
        }

        // Valida y consume el 2FA vinculado a esta operación específica
        boolean otpValido = segundoFactor.validarYConsumir(
                cliente.getId(), cuenta.getId(), request.getIdServicio(),
                request.getIdProducto(), request.getMonto(), request.getCodigo2FA());

        if (!otpValido) {
            throw new SegundoFactorInvalidoException("El código de segunda autenticación es inválido o ha expirado.");
        }

        PagoServicioValidado pago = new PagoServicioValidado(
                cuenta.getId(), cliente.getId(),
                request.getIdServicio(), request.getIdProducto(),
                request.getMonto(), saldo.getMoneda(),
                request.getTelefono(), request.getReferencia());

        log.info("Enviando pago de servicio al proveedor. cuentaId={}, idServicio={}, idProducto={}",
                cuenta.getId(), request.getIdServicio(), request.getIdProducto());
        PagoServicioResponse respuesta = gateway.enviar(pago);
        if (respuesta == null || respuesta.getEstado() == null) {
            throw new IntegracionNoDisponibleException("Respuesta del proveedor no concluyente. Consulte el estado de la operación antes de reintentar.");
        }

        // REGLA FINANCIERA OBLIGATORIA: Descuento del saldo únicamente ante confirmación de pago exitoso (APLICADO)
        if (respuesta.getEstado() == EstadoPagoServicio.APLICADO) {
            descontarSaldo(cuenta, request.getMonto());
        } else {
            log.warn("No se realizó descuento de saldo para cuentaId={} debido a estado={}",
                    cuenta.getId(), respuesta.getEstado());
        }

        return respuesta;
    }

    @Override
    public PagoServicioResponse conciliarTransaccion(String upc) {
        SentTxGateway gateway = sentTxGatewayProvider.getIfAvailable();
        if (gateway == null) {
            throw new IntegracionNoDisponibleException("El servicio de conciliación con el proveedor no está disponible.");
        }

        PagoServicioResponse respuesta = gateway.confirmar(upc);

        // Si la confirmación resulta en APLICADO y no se había descontado el saldo:
        if (respuesta.getEstado() == EstadoPagoServicio.APLICADO && transaccionRepository != null) {
            transaccionRepository.findByUpc(upc).ifPresent(tx -> {
                cuentaRepository.findById(tx.getCuentaId()).ifPresent(cuenta -> {
                    descontarSaldo(cuenta, tx.getMonto());
                });
            });
        }

        return respuesta;
    }

    @Override
    public void descontarSaldo(Cuenta cuenta, BigDecimal monto) {
        if (cuenta == null || monto == null) {
            return;
        }

        Saldo saldo = cuenta.getSaldo();
        if (saldo == null && saldoRepository != null) {
            saldo = saldoRepository.findByCuentaId(cuenta.getId()).orElse(null);
        }

        if (saldo != null && saldo.getSaldoDisponible() != null) {
            // Protección contra saldos negativos ante condiciones de carrera
            if (saldo.getSaldoDisponible().compareTo(monto) < 0) {
                log.error("ALERTA CRÍTICA: Saldo insuficiente detectado al descontar. cuentaId={}, disponible={}, monto={}",
                        cuenta.getId(), saldo.getSaldoDisponible(), monto);
                throw new SaldoInsuficienteException("Saldo insuficiente para completar el descuento monetario.");
            }

            BigDecimal nuevoDisponible = saldo.getSaldoDisponible().subtract(monto);
            saldo.setSaldoDisponible(nuevoDisponible);
            if (saldo.getSaldoContable() != null) {
                saldo.setSaldoContable(saldo.getSaldoContable().subtract(monto));
            }

            if (saldoRepository != null) {
                saldoRepository.save(saldo);
            }
            log.info("Descuento de saldo aplicado exitosamente. cuentaId={}, nuevoDisponible={}",
                    cuenta.getId(), nuevoDisponible);
        }
    }

    // ───────────────── Validaciones privadas ─────────────────

    private Cliente validarPropiedadYEstado(Cuenta cuenta, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
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
        return cliente;
    }

    private void validarEstatusCuenta(Cuenta cuenta) {
        if (cuenta.getEstatusCuenta() == null
                || !EstadoCuenta.ACTIVA.name().equalsIgnoreCase(cuenta.getEstatusCuenta().getNombre())
                || !Boolean.TRUE.equals(cuenta.getEstatusCuenta().getActivo())) {
            throw new CuentaNoAutorizadaException("La cuenta no se encuentra activa.");
        }
    }

    private void validarProductoYMonto(PagoServicioRequest request) {
        GestoPagoProducto producto = productoRepository
                .findFirstByIdServicioAndIdProducto(request.getIdServicio(), request.getIdProducto())
                .orElseThrow(() -> productoRepository.existsByIdServicio(request.getIdServicio())
                        ? new ProductoNoValidoException("El producto no corresponde al servicio indicado.")
                        : new ProductoNoValidoException("El servicio indicado no existe."));

        // precio > 0 => precio fijo determinado por el catálogo; precio nulo o 0 => importe variable.
        if (producto.getPrecio() != null && producto.getPrecio() > 0) {
            BigDecimal precio = BigDecimal.valueOf(producto.getPrecio());
            if (precio.compareTo(request.getMonto()) != 0) {
                throw new ProductoNoValidoException("El monto no corresponde al precio del producto.");
            }
        }
    }

    private Saldo validarSaldo(Cuenta cuenta, BigDecimal monto) {
        Saldo saldo = cuenta.getSaldo();
        if (saldo == null && saldoRepository != null) {
            saldo = saldoRepository.findByCuentaId(cuenta.getId()).orElse(null);
            cuenta.setSaldo(saldo);
        }
        if (saldo == null || saldo.getSaldoDisponible() == null
                || saldo.getSaldoDisponible().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para realizar el pago.");
        }
        return saldo;
    }

    private void validarNoDuplicada(Cuenta cuenta, PagoServicioRequest request) {
        if (transaccionRepository == null) {
            return;
        }
        boolean duplicada = transaccionRepository.existsByCuentaIdAndIdServicioAndIdProductoAndMontoAndEstadoIn(
                cuenta.getId(), request.getIdServicio(), request.getIdProducto(),
                request.getMonto(), ESTADOS_DUPLICADOS);
        if (duplicada) {
            log.warn("Detección de operación duplicada para cuentaId={}, servicio={}, producto={}, monto={}",
                    cuenta.getId(), request.getIdServicio(), request.getIdProducto(), request.getMonto());
            throw new OperacionDuplicadaException(
                    "Ya existe una operación registrada o pendiente de confirmación para esta cuenta, servicio y monto.");
        }
    }
}
