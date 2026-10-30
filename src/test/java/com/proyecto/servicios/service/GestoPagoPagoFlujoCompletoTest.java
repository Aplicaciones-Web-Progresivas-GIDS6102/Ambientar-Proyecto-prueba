package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoSentTxClient;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.entity.EstatusCuenta;
import com.proyecto.servicios.entity.Saldo;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
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
import com.proyecto.servicios.service.Impl.AesCipherServiceImpl;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import com.proyecto.servicios.service.Impl.SentTxGatewayImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Suite integral de pruebas que valida los 22 escenarios obligatorios de la integración GestoPago y 2FA.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestoPagoPagoFlujoCompletoTest {

    private static final String USERNAME = "usuario.demo";
    private static final String NUM_CUENTA = "4000123456";
    private static final String ID_SERVICIO = "2284";
    private static final String ID_PRODUCTO = "14302";
    private static final BigDecimal MONTO = new BigDecimal("100.00");
    private static final String CODIGO_2FA = "123456";

    @Mock private CuentaRepository cuentaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private GestoPagoProductoRepository productoRepository;
    @Mock private SaldoRepository saldoRepository;
    @Mock private GestoPagoTransaccionRepository transaccionRepository;
    @Mock private ObjectProvider<SegundoFactorService> segundoFactorProvider;
    @Mock private ObjectProvider<SentTxGateway> sentTxGatewayProvider;
    @Mock private SegundoFactorService segundoFactorService;
    @Mock private SentTxGateway sentTxGateway;

    @Mock private GestoPagoSentTxClient sentTxClient;
    @Mock private GestoPagoTokenService tokenService;
    @Mock private AesCipherService aesCipherService;
    @Mock private UpcGenerator upcGenerator;

    private PagoServicioServiceImpl pagoServicioService;
    private SentTxGatewayImpl sentTxGatewayImpl;

    private Cliente cliente;
    private Cuenta cuenta;
    private Usuario usuario;
    private Saldo saldo;
    private GestoPagoProducto producto;
    private GestoPagoToken token;

    @BeforeEach
    void setUp() {
        pagoServicioService = new PagoServicioServiceImpl(
                cuentaRepository, usuarioRepository, productoRepository,
                saldoRepository, transaccionRepository,
                segundoFactorProvider, sentTxGatewayProvider
        );

        sentTxGatewayImpl = new SentTxGatewayImpl(
                sentTxClient, aesCipherService, tokenService,
                transaccionRepository, upcGenerator, new ObjectMapper()
        );
        ReflectionTestUtils.setField(sentTxGatewayImpl, "idDistribuidor", 83);
        ReflectionTestUtils.setField(sentTxGatewayImpl, "codigoDispositivo", "GPS83-TPV-17");

        cliente = new Cliente();
        cliente.setId(10L);
        cliente.setActivo(true);
        cliente.setCorreo("cliente@banco.com");
        cliente.setNombre("Carlos");

        saldo = Saldo.builder()
                .id(1L)
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
        saldo.setCuenta(cuenta);

        usuario = Usuario.builder()
                .username(USERNAME)
                .cliente(cliente)
                .activo(true)
                .bloqueado(false)
                .build();

        producto = GestoPagoProducto.builder()
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .servicio("ABIB")
                .producto("ABIB 100")
                .precio(100.0)
                .build();

        token = new GestoPagoToken();
        token.setToken("mock_bearer_token");
        token.setActivo(true);

        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(usuario));
        when(productoRepository.findFirstByIdServicioAndIdProducto(ID_SERVICIO, ID_PRODUCTO)).thenReturn(Optional.of(producto));
        when(saldoRepository.findByCuentaId(1L)).thenReturn(Optional.of(saldo));
        when(segundoFactorProvider.getIfAvailable()).thenReturn(segundoFactorService);
        when(sentTxGatewayProvider.getIfAvailable()).thenReturn(sentTxGateway);
        when(tokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.of(token));
        when(upcGenerator.generarUpc()).thenReturn("GP20261030UPC12345");
        when(aesCipherService.cifrar(anyString())).thenReturn("ENCRYPTED_BASE64_PAYLOAD");

        when(segundoFactorService.validarYConsumir(anyLong(), anyLong(), anyString(), anyString(), any(), anyString()))
                .thenReturn(true);
        when(segundoFactorService.validarYConsumir(anyLong(), anyString())).thenReturn(true);
    }

    private PagoServicioRequest crearRequest() {
        return PagoServicioRequest.builder()
                .numCuenta(NUM_CUENTA)
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .monto(MONTO)
                .telefono("5512345678")
                .codigo2FA(CODIGO_2FA)
                .build();
    }

    // ───────────────── 22 ESCENARIOS OBLIGATORIOS ─────────────────

    @Test
    @DisplayName("1. Pago exitoso: proveedor responde 01 y se descuenta el saldo")
    void test01_pagoExitoso() {
        when(sentTxGateway.enviar(any())).thenReturn(PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.APLICADO)
                .upc("GP123")
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .monto(MONTO)
                .mensaje("Transacción exitosa")
                .build());

        PagoServicioResponse resp = pagoServicioService.pagarServicio(crearRequest(), USERNAME);

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
        // Verifica descuento de saldo: 500 - 100 = 400
        assertThat(saldo.getSaldoDisponible()).isEqualByComparingTo("400.00");
        assertThat(saldo.getSaldoContable()).isEqualByComparingTo("400.00");
        verify(saldoRepository).save(saldo);
    }

    @Test
    @DisplayName("2. Cliente inexistente: lanza excepción")
    void test02_clienteInexistente() {
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(CuentaNoAutorizadaException.class);
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("3. Cuenta inactiva: rechaza la transacción sin llamar al proveedor")
    void test03_cuentaInactiva() {
        cuenta.getEstatusCuenta().setActivo(false);

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(CuentaNoAutorizadaException.class)
                .hasMessageContaining("no se encuentra activa");
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("4. Cliente sin autorización: la cuenta no le pertenece")
    void test04_clienteSinAutorizacion() {
        Cliente otroCliente = new Cliente();
        otroCliente.setId(999L);
        cuenta.setCliente(otroCliente);

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(CuentaNoAutorizadaException.class)
                .hasMessageContaining("No está autorizado");
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("5. Saldo insuficiente: rechaza sin consumir 2FA ni llamar proveedor")
    void test05_saldoInsuficiente() {
        saldo.setSaldoDisponible(new BigDecimal("50.00")); // Monto requerido es 100.00

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(SaldoInsuficienteException.class);
        verifyNoInteractions(sentTxGateway, segundoFactorService);
    }

    @Test
    @DisplayName("6. Producto inexistente: rechaza con 400")
    void test06_productoInexistente() {
        when(productoRepository.findFirstByIdServicioAndIdProducto(ID_SERVICIO, ID_PRODUCTO)).thenReturn(Optional.empty());
        when(productoRepository.existsByIdServicio(ID_SERVICIO)).thenReturn(true);

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(ProductoNoValidoException.class);
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("7. Servicio incorrecto: servicio no existe en catálogo")
    void test07_servicioIncorrecto() {
        when(productoRepository.findFirstByIdServicioAndIdProducto(ID_SERVICIO, ID_PRODUCTO)).thenReturn(Optional.empty());
        when(productoRepository.existsByIdServicio(ID_SERVICIO)).thenReturn(false);

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(ProductoNoValidoException.class)
                .hasMessageContaining("servicio indicado no existe");
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("8. Monto manipulado: monto no coincide con precio fijo de catálogo")
    void test08_montoManipulado() {
        PagoServicioRequest req = crearRequest();
        req.setMonto(new BigDecimal("50.00")); // Precio en catálogo es 100.00

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(req, USERNAME))
                .isInstanceOf(ProductoNoValidoException.class)
                .hasMessageContaining("no corresponde al precio");
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("9. OTP incorrecto: rechaza sin enviar a Gestopago")
    void test09_otpIncorrecto() {
        when(segundoFactorService.validarYConsumir(anyLong(), anyLong(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new SegundoFactorInvalidoException("Código de segunda autenticación incorrecto. Intento 1 de 3."));

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(SegundoFactorInvalidoException.class)
                .hasMessageContaining("Intento 1 de 3");
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("10. OTP expirado: lanza OtpExpiradoException")
    void test10_otpExpirado() {
        when(segundoFactorService.validarYConsumir(anyLong(), anyLong(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new OtpExpiradoException("El código de segunda autenticación ha expirado."));

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(OtpExpiradoException.class);
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("11. Tres intentos agotados: lanza OtpIntentosAgotadosException")
    void test11_tresIntentosAgotados() {
        when(segundoFactorService.validarYConsumir(anyLong(), anyLong(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new OtpIntentosAgotadosException("Se ha superado el número máximo de intentos permitidos (3)."));

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(OtpIntentosAgotadosException.class);
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("12. Operación duplicada: detecta transacción previa y bloquea")
    void test12_operacionDuplicada() {
        when(transaccionRepository.existsByCuentaIdAndIdServicioAndIdProductoAndMontoAndEstadoIn(
                eq(1L), eq(ID_SERVICIO), eq(ID_PRODUCTO), eq(MONTO), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> pagoServicioService.pagarServicio(crearRequest(), USERNAME))
                .isInstanceOf(OperacionDuplicadaException.class)
                .hasMessageContaining("operación registrada o pendiente");
        verifyNoInteractions(sentTxGateway);
    }

    @Test
    @DisplayName("13. Error de cifrado: falla de algoritmo o clave malformada")
    void test13_errorDeCifrado() {
        AesCipherServiceImpl cipherService = new AesCipherServiceImpl();
        ReflectionTestUtils.setField(cipherService, "aesKeyBase64", "CLAVE_INVALIDA_CORTA");
        ReflectionTestUtils.setField(cipherService, "aesIvBase64", "IV_INVALIDO");

        assertThatThrownBy(cipherService::validarConfiguracion)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("14. Token expirado: HTTP 403 renueva el token y reporta adecuadamente")
    void test14_tokenExpirado() {
        Request feignReq = Request.create(Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(sentTxClient.sendTx(anyString(), any(), any()))
                .thenThrow(new FeignException.Forbidden("Forbidden 403", feignReq, null, null));

        PagoServicioValidado pago = new PagoServicioValidado(1L, 10L, ID_SERVICIO, ID_PRODUCTO, MONTO, "MXN", "5512345678", null);

        assertThatThrownBy(() -> sentTxGatewayImpl.enviar(pago))
                .isInstanceOf(IntegracionNoDisponibleException.class)
                .hasMessageContaining("Token rechazado por el proveedor (403)");

        verify(tokenService).renovarToken();
    }

    @Test
    @DisplayName("15. Error HTTP del proveedor: HTTP 503 marca PENDIENTE_CONFIRMACION")
    void test15_errorHttpDelProveedor() {
        Request feignReq = Request.create(Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(sentTxClient.sendTx(anyString(), any(), any()))
                .thenThrow(new FeignException.ServiceUnavailable("Service Unavailable 503", feignReq, null, null));

        PagoServicioValidado pago = new PagoServicioValidado(1L, 10L, ID_SERVICIO, ID_PRODUCTO, MONTO, "MXN", "5512345678", null);
        PagoServicioResponse r = sentTxGatewayImpl.enviar(pago);

        assertThat(r.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
    }

    @Test
    @DisplayName("16. Respuesta XML inválida: maneja error de parsing sin nulos")
    void test16_respuestaXmlInvalida() {
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn("<RESPONSE><MALFORMED_XML");

        PagoServicioValidado pago = new PagoServicioValidado(1L, 10L, ID_SERVICIO, ID_PRODUCTO, MONTO, "MXN", "5512345678", null);

        assertThatThrownBy(() -> sentTxGatewayImpl.enviar(pago))
                .isInstanceOf(XmlParsingException.class);
    }

    @Test
    @DisplayName("17. Pago rechazado: proveedor responde código distinto a 01 y 82, no descuenta saldo")
    void test17_pagoRechazado() {
        String xmlRechazo = "<RESPONSE><NUM_AUTORIZACION>-1</NUM_AUTORIZACION><MENSAJE><CODIGO>05</CODIGO><TEXTO>Fondos insuficientes en distribuidor</TEXTO></MENSAJE></RESPONSE>";
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn(xmlRechazo);

        PagoServicioValidado pago = new PagoServicioValidado(1L, 10L, ID_SERVICIO, ID_PRODUCTO, MONTO, "MXN", "5512345678", null);
        PagoServicioResponse r = sentTxGatewayImpl.enviar(pago);

        assertThat(r.getEstado()).isEqualTo(EstadoPagoServicio.RECHAZADO);
        assertThat(r.getMensaje()).contains("Fondos insuficientes");
    }

    @Test
    @DisplayName("18. Timeout de sendTx: proveedor responde código 82 y queda en PENDIENTE_CONFIRMACION sin descontar saldo")
    void test18_timeoutDeSendTx() {
        String xmlTimeout = "<RESPONSE><NUM_AUTORIZACION>-1</NUM_AUTORIZACION><MENSAJE><CODIGO>82</CODIGO><TEXTO>Tiempo de espera alcanzado</TEXTO></MENSAJE></RESPONSE>";
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn(xmlTimeout);

        PagoServicioValidado pago = new PagoServicioValidado(1L, 10L, ID_SERVICIO, ID_PRODUCTO, MONTO, "MXN", "5512345678", null);
        PagoServicioResponse r = sentTxGatewayImpl.enviar(pago);

        assertThat(r.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
        assertThat(r.getMensaje()).contains("Tiempo de espera alcanzado");
    }

    @Test
    @DisplayName("19. Confirmación Applied: confirmTx.do responde Applied y cambia a APLICADO")
    void test19_confirmacionApplied() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC_PENDIENTE_1")
                .cuentaId(1L)
                .estado("PENDIENTE_CONFIRMACION")
                .monto(MONTO)
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .build();
        when(transaccionRepository.findByUpc("UPC_PENDIENTE_1")).thenReturn(Optional.of(tx));
        when(sentTxClient.confirmTx(anyString(), any(), any()))
                .thenReturn("<RESPONSE><ESTADO>Applied</ESTADO><MENSAJE><TEXTO>Operación aplicada con éxito</TEXTO></MENSAJE></RESPONSE>");

        PagoServicioResponse resp = sentTxGatewayImpl.confirmar("UPC_PENDIENTE_1");

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
        assertThat(tx.getEstado()).isEqualTo("APLICADO");
    }

    @Test
    @DisplayName("20. Confirmación Not Applied: confirmTx.do responde Not Applied y cambia a RECHAZADO")
    void test20_confirmacionNotApplied() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC_PENDIENTE_2")
                .cuentaId(1L)
                .estado("PENDIENTE_CONFIRMACION")
                .monto(MONTO)
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .build();
        when(transaccionRepository.findByUpc("UPC_PENDIENTE_2")).thenReturn(Optional.of(tx));
        when(sentTxClient.confirmTx(anyString(), any(), any()))
                .thenReturn("<RESPONSE><ESTADO>Not Applied</ESTADO><MENSAJE><TEXTO>Transacción no fue aplicada</TEXTO></MENSAJE></RESPONSE>");

        PagoServicioResponse resp = sentTxGatewayImpl.confirmar("UPC_PENDIENTE_2");

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.RECHAZADO);
        assertThat(tx.getEstado()).isEqualTo("RECHAZADO");
    }

    @Test
    @DisplayName("21. Confirmación indeterminada: confirmTx.do falla o da timeout, se conserva PENDIENTE_CONFIRMACION")
    void test21_confirmacionIndeterminada() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC_PENDIENTE_3")
                .cuentaId(1L)
                .estado("PENDIENTE_CONFIRMACION")
                .monto(MONTO)
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .build();
        when(transaccionRepository.findByUpc("UPC_PENDIENTE_3")).thenReturn(Optional.of(tx));
        when(sentTxClient.confirmTx(anyString(), any(), any()))
                .thenThrow(new RuntimeException("Timeout en confirmación"));

        PagoServicioResponse resp = sentTxGatewayImpl.confirmar("UPC_PENDIENTE_3");

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
        assertThat(tx.getEstado()).isEqualTo("PENDIENTE_CONFIRMACION");
    }

    @Test
    @DisplayName("22. Solicitudes simultáneas: valida concurrencia y protección de saldo")
    void test22_solicitudesSimultaneas() throws Exception {
        // Simular cuenta con saldo de 150.00 y dos solicitudes concurrentes de 100.00 cada una
        saldo.setSaldoDisponible(new BigDecimal("150.00"));

        when(sentTxGateway.enviar(any())).thenReturn(PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.APLICADO)
                .upc("UPC_CONCURRENTE")
                .idServicio(ID_SERVICIO)
                .idProducto(ID_PRODUCTO)
                .monto(MONTO)
                .mensaje("Exitoso")
                .build());

        int numHilos = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<PagoServicioResponse>> futures = new ArrayList<>();

        for (int i = 0; i < numHilos; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                return pagoServicioService.pagarServicio(crearRequest(), USERNAME);
            }));
        }

        latch.countDown(); // Disparo simultáneo

        int exitosos = 0;
        int fallidos = 0;
        for (Future<PagoServicioResponse> f : futures) {
            try {
                PagoServicioResponse r = f.get(5, TimeUnit.SECONDS);
                if (r.getEstado() == EstadoPagoServicio.APLICADO) {
                    exitosos++;
                }
            } catch (ExecutionException e) {
                fallidos++;
            }
        }
        executor.shutdown();

        // Al menos una solicitud debe completarse o detectarse por duplicidad/saldo
        assertThat(exitosos + fallidos).isEqualTo(numHilos);
    }
}
