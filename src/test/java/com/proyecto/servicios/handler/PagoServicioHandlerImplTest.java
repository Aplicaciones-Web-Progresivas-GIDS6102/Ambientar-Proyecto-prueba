package com.proyecto.servicios.handler;

import com.proyecto.servicios.dto.Solicitud2FARequestDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.EstatusCuenta;
import com.proyecto.servicios.entity.Saldo;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import com.proyecto.servicios.exception.ProductoNoValidoException;
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
}
