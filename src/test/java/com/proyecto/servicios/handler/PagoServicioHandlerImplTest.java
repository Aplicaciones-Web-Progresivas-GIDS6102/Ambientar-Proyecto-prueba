package com.proyecto.servicios.handler;

import com.proyecto.servicios.dto.Solicitud2FARequestDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.EstatusCuenta;
import com.proyecto.servicios.entity.Saldo;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import com.proyecto.servicios.exception.CuentaNoAutorizadaException;
import com.proyecto.servicios.exception.ProductoNoValidoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.handler.Impl.PagoServicioHandlerImpl;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.SaldoRepository;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.service.SegundoFactorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServicioHandlerImplTest {

    @Mock private PagoServicioService pagoServicioService;
    @Mock private SegundoFactorService segundoFactorService;
    @Mock private CuentaRepository cuentaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private GestoPagoProductoRepository productoRepository;
    @Mock private SaldoRepository saldoRepository;

    private PagoServicioHandlerImpl handler;

    private Cliente cliente;
    private Cuenta cuenta;
    private Usuario usuario;
    private Saldo saldo;
    private GestoPagoProducto producto;

    private static final String USERNAME = "carlos.user";
    private static final String NUM_CUENTA = "4000123456";
    private static final String ID_SERVICIO = "2284";
    private static final String ID_PRODUCTO = "14302";
    private static final BigDecimal MONTO = new BigDecimal("100.00");

    @BeforeEach
    void setUp() {
        handler = new PagoServicioHandlerImpl(
                pagoServicioService, segundoFactorService, cuentaRepository,
                usuarioRepository, productoRepository, saldoRepository
        );

        cliente = new Cliente();
        cliente.setId(10L);
        cliente.setActivo(true);
        cliente.setCorreo("carlos@banco.com");
        cliente.setNombre("Carlos");
        cliente.setApellidoPaterno("Hernandez");

        saldo = Saldo.builder()
                .saldoDisponible(new BigDecimal("500.00"))
                .saldoContable(new BigDecimal("500.00"))
                .moneda("MXN")
                .build();

        cuenta = Cuenta.builder()
                .id(1L)
                .numeroCuenta(NUM_CUENTA)
                .cliente(cliente)
                .saldo(saldo)
                .estatusCuenta(EstatusCuenta.builder().nombre("ACTIVA").activo(true).build())
                .build();

        usuario = Usuario.builder()
                .username(USERNAME)
                .cliente(cliente)
                .activo(true)
                .bloqueado(false)
                .build();

        producto = GestoPagoProducto.builder()
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .precio(100.0)
                .build();
    }

    @Test
    @DisplayName("solicitar2FA valida cuenta, saldo, producto y envía OTP")
    void solicitar2FA_Exitoso() {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder()
                .numCuenta(NUM_CUENTA)
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .monto(MONTO)
                .build();

        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(productoRepository.findFirstByIdServicioAndIdProducto(ID_SERVICIO, ID_PRODUCTO)).thenReturn(Optional.of(producto));
        when(segundoFactorService.solicitarOtp(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyString(), any()))
                .thenReturn(300);

        GenericResponse resp = handler.solicitar2FA(req, USERNAME);

        assertThat(resp.getCodigo()).isZero();
        assertThat(resp.getMensaje()).contains("Código de segunda autenticación enviado");
        verify(segundoFactorService).solicitarOtp(eq(10L), eq("carlos@banco.com"), anyString(), eq(1L), eq(ID_SERVICIO), eq(ID_PRODUCTO), eq(MONTO));
    }

    @Test
    @DisplayName("procesarPago exitoso retorna HTTP 200 y código interno 0")
    void procesarPago_Aplicado_RetornaCodigoCero() {
        PagoServicioRequest req = PagoServicioRequest.builder()
                .numCuenta(NUM_CUENTA)
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .monto(MONTO)
                .telefono("5512345678")
                .codigo2FA("123456")
                .build();

        PagoServicioResponse pagoResp = PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.APLICADO)
                .upc("UPC123")
                .monto(MONTO)
                .mensaje("Transacción exitosa")
                .build();

        when(pagoServicioService.pagarServicio(req, USERNAME)).thenReturn(pagoResp);

        ResponseEntity<GenericResponse> response = handler.procesarPago(req, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCodigo()).isZero();
        assertThat(response.getBody().getMensaje()).isEqualTo("Transacción exitosa");
    }

    @Test
    @DisplayName("procesarPago en timeout retorna HTTP 202 ACCEPTED y código interno 82")
    void procesarPago_Timeout_Retorna202YCodigo82() {
        PagoServicioRequest req = PagoServicioRequest.builder()
                .numCuenta(NUM_CUENTA)
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .monto(MONTO)
                .telefono("5512345678")
                .codigo2FA("123456")
                .build();

        PagoServicioResponse pagoResp = PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.PENDIENTE_CONFIRMACION)
                .upc("UPC123")
                .monto(MONTO)
                .mensaje("Tiempo de espera alcanzado")
                .build();

        when(pagoServicioService.pagarServicio(req, USERNAME)).thenReturn(pagoResp);

        ResponseEntity<GenericResponse> response = handler.procesarPago(req, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCodigo()).isEqualTo(82);
    }

    @Test
    @DisplayName("procesarPago rechazado retorna HTTP 422 y código 422")
    void procesarPago_Rechazado() {
        PagoServicioRequest req = PagoServicioRequest.builder().build();
        PagoServicioResponse pagoResp = PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.RECHAZADO)
                .mensaje("Rechazado")
                .build();
        when(pagoServicioService.pagarServicio(any(), any())).thenReturn(pagoResp);

        ResponseEntity<GenericResponse> response = handler.procesarPago(req, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().getCodigo()).isEqualTo(422);
    }

    @Test
    @DisplayName("conciliarTransaccion para estados APLICADO, PENDIENTE y RECHAZADO")
    void conciliarTransaccion_Estados() {
        when(pagoServicioService.conciliarTransaccion("UPC1")).thenReturn(
                PagoServicioResponse.builder().estado(EstadoPagoServicio.APLICADO).build());
        when(pagoServicioService.conciliarTransaccion("UPC2")).thenReturn(
                PagoServicioResponse.builder().estado(EstadoPagoServicio.PENDIENTE_CONFIRMACION).build());
        when(pagoServicioService.conciliarTransaccion("UPC3")).thenReturn(
                PagoServicioResponse.builder().estado(EstadoPagoServicio.RECHAZADO).build());

        GenericResponse r1 = handler.conciliarTransaccion("UPC1");
        GenericResponse r2 = handler.conciliarTransaccion("UPC2");
        GenericResponse r3 = handler.conciliarTransaccion("UPC3");

        assertThat(r1.getCodigo()).isZero();
        assertThat(r2.getCodigo()).isEqualTo(82);
        assertThat(r3.getCodigo()).isEqualTo(422);
    }

    @Test
    @DisplayName("solicitar2FA falla si usuario está bloqueado o inactivo")
    void solicitar2FA_UsuarioBloqueado() {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder().numCuenta(NUM_CUENTA).build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        usuario.setBloqueado(true);
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));

        assertThrows(CuentaNoAutorizadaException.class, () -> handler.solicitar2FA(req, USERNAME));
    }

    @Test
    @DisplayName("solicitar2FA falla si cliente no coincide o está inactivo")
    void solicitar2FA_ClienteInactivo() {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder().numCuenta(NUM_CUENTA).build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        cliente.setActivo(false);
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));

        assertThrows(CuentaNoAutorizadaException.class, () -> handler.solicitar2FA(req, USERNAME));
    }

    @Test
    @DisplayName("solicitar2FA falla si estatus de cuenta no es activo")
    void solicitar2FA_CuentaNoActiva() {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder().numCuenta(NUM_CUENTA).build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        cuenta.getEstatusCuenta().setActivo(false);
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));

        assertThrows(CuentaNoAutorizadaException.class, () -> handler.solicitar2FA(req, USERNAME));
    }

    @Test
    @DisplayName("solicitar2FA falla cuando servicio no existe en catálogo")
    void solicitar2FA_ServicioInexistente() {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder()
                .numCuenta(NUM_CUENTA).idServicio("9999").idProducto("1111").monto(MONTO).build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(productoRepository.findFirstByIdServicioAndIdProducto("9999", "1111")).thenReturn(Optional.empty());
        when(productoRepository.existsByIdServicio("9999")).thenReturn(false);

        assertThrows(ProductoNoValidoException.class, () -> handler.solicitar2FA(req, USERNAME));
    }

    @Test
    @DisplayName("solicitar2FA falla cuando producto no corresponde a servicio existente")
    void solicitar2FA_ProductoNoCorresponde() {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder()
                .numCuenta(NUM_CUENTA).idServicio(ID_SERVICIO).idProducto("99999").monto(MONTO).build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(productoRepository.findFirstByIdServicioAndIdProducto(ID_SERVICIO, "99999")).thenReturn(Optional.empty());
        when(productoRepository.existsByIdServicio(ID_SERVICIO)).thenReturn(true);

        assertThrows(ProductoNoValidoException.class, () -> handler.solicitar2FA(req, USERNAME));
    }

    @Test
    @DisplayName("solicitar2FA falla cuando monto no corresponde al precio fijo")
    void solicitar2FA_MontoNoCoincide() {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder()
                .numCuenta(NUM_CUENTA).idServicio(ID_SERVICIO).idProducto(ID_PRODUCTO).monto(new BigDecimal("50.00")).build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(productoRepository.findFirstByIdServicioAndIdProducto(ID_SERVICIO, ID_PRODUCTO)).thenReturn(Optional.of(producto));

        assertThrows(ProductoNoValidoException.class, () -> handler.solicitar2FA(req, USERNAME));
    }

    @Test
    @DisplayName("solicitar2FA busca saldo en repositorio cuando cuenta.getSaldo() es nulo y falla si insuficiente")
    void solicitar2FA_SaldoDesdeRepositorioInsuficiente() {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder()
                .numCuenta(NUM_CUENTA).idServicio(ID_SERVICIO).idProducto(ID_PRODUCTO).monto(MONTO).build();
        cuenta.setSaldo(null);
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(productoRepository.findFirstByIdServicioAndIdProducto(ID_SERVICIO, ID_PRODUCTO)).thenReturn(Optional.of(producto));
        when(saldoRepository.findByCuentaId(1L)).thenReturn(Optional.of(Saldo.builder().saldoDisponible(new BigDecimal("10.00")).build()));

        assertThrows(SaldoInsuficienteException.class, () -> handler.solicitar2FA(req, USERNAME));
    }

    @Test
    @DisplayName("solicitar2FA con correo corto o inválido prueba ramas de enmascaramiento")
    void solicitar2FA_CorreoCorto() {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder()
                .numCuenta(NUM_CUENTA).idServicio(ID_SERVICIO).idProducto(ID_PRODUCTO).monto(MONTO).build();
        cliente.setCorreo("ab@test.com");
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(productoRepository.findFirstByIdServicioAndIdProducto(ID_SERVICIO, ID_PRODUCTO)).thenReturn(Optional.of(producto));
        when(segundoFactorService.solicitarOtp(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyString(), any()))
                .thenReturn(300);

        GenericResponse resp = handler.solicitar2FA(req, USERNAME);
        assertThat(resp).isNotNull();
    }
}
