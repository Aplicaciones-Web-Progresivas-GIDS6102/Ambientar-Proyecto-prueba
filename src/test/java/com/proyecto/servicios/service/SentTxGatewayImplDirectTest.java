package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoSentTxClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import com.proyecto.servicios.exception.IntegracionNoDisponibleException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioValidado;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.service.Impl.SentTxGatewayImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SentTxGatewayImplDirectTest {

    @Mock
    private GestoPagoSentTxClient sentTxClient;

    @Mock
    private AesCipherService aesCipherService;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private GestoPagoTransaccionRepository transaccionRepository;

    @Mock
    private UpcGenerator upcGenerator;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SentTxGatewayImpl gateway;

    private PagoServicioValidado pago;

    @BeforeEach
    void setUp() {
        pago = new PagoServicioValidado(
                1L, 10L, "2284", "14302", new BigDecimal("100.00"), "MXN", "5512345678", "REF123"
        );
        ReflectionTestUtils.setField(gateway, "idDistribuidor", 83);
        ReflectionTestUtils.setField(gateway, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(gateway, "apiKey", "testApiKey");
        ReflectionTestUtils.setField(gateway, "simulacionExitosa", false);

        GestoPagoToken token = new GestoPagoToken();
        token.setToken("valid_token");
        when(tokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(token));
        when(aesCipherService.cifrar(anyString())).thenReturn("cifrado");
    }

    @Test
    @DisplayName("Modo simulación exitosa al enviar pago")
    void enviar_ModoSimulacion() {
        ReflectionTestUtils.setField(gateway, "simulacionExitosa", true);
        when(upcGenerator.generarUpc()).thenReturn("UPC123");

        PagoServicioResponse response = gateway.enviar(pago);

        assertThat(response.getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
        verify(transaccionRepository, atLeastOnce()).save(any(GestoPagoTransaccion.class));
    }

    @Test
    @DisplayName("Modo simulación exitosa al confirmar")
    void confirmar_ModoSimulacion() {
        ReflectionTestUtils.setField(gateway, "simulacionExitosa", true);
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC123")
                .estado("PENDIENTE")
                .monto(new BigDecimal("100.00"))
                .build();
        when(transaccionRepository.findByUpc("UPC123")).thenReturn(Optional.of(tx));

        PagoServicioResponse response = gateway.confirmar("UPC123");

        assertThat(response.getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
    }

    @Test
    @DisplayName("Confirmar con UPC nulo o vacío lanza excepción")
    void confirmar_UpcInvalido() {
        assertThrows(ValidacionNegocioException.class, () -> gateway.confirmar(null));
        assertThrows(ValidacionNegocioException.class, () -> gateway.confirmar("   "));
    }

    @Test
    @DisplayName("Confirmar transacción no encontrada lanza excepción")
    void confirmar_NoEncontrada() {
        when(transaccionRepository.findByUpc("INEXISTENTE")).thenReturn(Optional.empty());
        assertThrows(ValidacionNegocioException.class, () -> gateway.confirmar("INEXISTENTE"));
    }

    @Test
    @DisplayName("Confirmar transacción ya APLICADA")
    void confirmar_YaAplicada() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC_YA_APLICADO")
                .estado("APLICADO")
                .monto(new BigDecimal("100.00"))
                .build();
        when(transaccionRepository.findByUpc("UPC_YA_APLICADO")).thenReturn(Optional.of(tx));

        PagoServicioResponse response = gateway.confirmar("UPC_YA_APLICADO");

        assertThat(response.getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
        verify(sentTxClient, never()).confirmTx(anyString(), any(), any());
    }

    @Test
    @DisplayName("Enviar con error 403 Forbidden del proveedor renueva token y lanza excepción")
    void enviar_Forbidden_RenuevaToken() {
        when(upcGenerator.generarUpc()).thenReturn("UPC_FORBIDDEN");

        Request request = Request.create(Request.HttpMethod.POST, "/url", Collections.emptyMap(), null, new RequestTemplate());
        when(sentTxClient.sendTx(anyString(), any(), any())).thenThrow(new FeignException.Forbidden("Forbidden", request, null, null));

        assertThrows(IntegracionNoDisponibleException.class, () -> gateway.enviar(pago));
        verify(tokenService, times(1)).renovarToken();
    }

    @Test
    @DisplayName("Enviar con FeignException (error 500) del proveedor retorna PENDIENTE_CONFIRMACION")
    void enviar_FeignException_RetornaPendiente() {
        when(upcGenerator.generarUpc()).thenReturn("UPC_500");

        Request request = Request.create(Request.HttpMethod.POST, "/url", Collections.emptyMap(), null, new RequestTemplate());
        when(sentTxClient.sendTx(anyString(), any(), any())).thenThrow(new FeignException.InternalServerError("500 Error", request, null, null));

        PagoServicioResponse resp = gateway.enviar(pago);

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
    }

    @Test
    @DisplayName("Enviar con excepción genérica retorna PENDIENTE_CONFIRMACION")
    void enviar_ExcepcionGenerica_RetornaPendiente() {
        when(upcGenerator.generarUpc()).thenReturn("UPC_EX");
        when(sentTxClient.sendTx(anyString(), any(), any())).thenThrow(new RuntimeException("Connection dropped"));

        PagoServicioResponse resp = gateway.enviar(pago);

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
    }

    @Test
    @DisplayName("Enviar cuando proveedor devuelve respuesta nula o vacía")
    void enviar_RespuestaVacia() {
        when(upcGenerator.generarUpc()).thenReturn("UPC_VACIO");
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn("");

        PagoServicioResponse resp = gateway.enviar(pago);

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
    }

    @Test
    @DisplayName("Enviar con respuesta XML exitosa (código 01)")
    void enviar_XmlExitoso() {
        when(upcGenerator.generarUpc()).thenReturn("UPC_XML_OK");
        String xml = "<RESPONSE><UPC>UPC_XML_OK</UPC><MENSAJE><CODIGO>01</CODIGO><TEXTO>Pago Exitoso</TEXTO></MENSAJE><NUM_AUTORIZACION>123456</NUM_AUTORIZACION></RESPONSE>";
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn(xml);

        PagoServicioResponse resp = gateway.enviar(pago);

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
    }

    @Test
    @DisplayName("Enviar con respuesta XML timeout (código 82)")
    void enviar_XmlTimeout() {
        when(upcGenerator.generarUpc()).thenReturn("UPC_XML_82");
        String xml = "<RESPONSE><UPC>UPC_XML_82</UPC><MENSAJE><CODIGO>82</CODIGO><TEXTO>Timeout no registrada</TEXTO></MENSAJE></RESPONSE>";
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn(xml);

        PagoServicioResponse resp = gateway.enviar(pago);

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
    }

    @Test
    @DisplayName("Enviar con respuesta XML de rechazo (código 99)")
    void enviar_XmlRechazado() {
        when(upcGenerator.generarUpc()).thenReturn("UPC_XML_99");
        String xml = "<RESPONSE><UPC>UPC_XML_99</UPC><MENSAJE><CODIGO>99</CODIGO><TEXTO>Saldo insuficiente</TEXTO></MENSAJE></RESPONSE>";
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn(xml);

        PagoServicioResponse resp = gateway.enviar(pago);

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.RECHAZADO);
    }

    @Test
    @DisplayName("Enviar con respuesta JSON exitosa")
    void enviar_JsonExitoso() {
        when(upcGenerator.generarUpc()).thenReturn("UPC_JSON_OK");
        String json = "{\"upc\":\"UPC_JSON_OK\",\"codigo\":\"01\",\"mensaje\":\"Applied\",\"data\":\"AUTH789\"}";
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn(json);

        PagoServicioResponse resp = gateway.enviar(pago);

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
    }

    @Test
    @DisplayName("Confirmar cuando proveedor devuelve respuesta nula o vacía")
    void confirmar_RespuestaVacia() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC_PEND")
                .estado("PENDIENTE")
                .monto(new BigDecimal("100.00"))
                .build();
        when(transaccionRepository.findByUpc("UPC_PEND")).thenReturn(Optional.of(tx));
        when(sentTxClient.confirmTx(anyString(), any(), any())).thenReturn("");

        PagoServicioResponse resp = gateway.confirmar("UPC_PEND");

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
    }

    @Test
    @DisplayName("Confirmar cuando proveedor lanza excepción en confirmTx.do")
    void confirmar_ErrorEnLlamada() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC_FAIL")
                .estado("PENDIENTE")
                .monto(new BigDecimal("100.00"))
                .build();
        when(transaccionRepository.findByUpc("UPC_FAIL")).thenReturn(Optional.of(tx));
        when(sentTxClient.confirmTx(anyString(), any(), any())).thenThrow(new RuntimeException("Timeout en confirmación"));

        PagoServicioResponse resp = gateway.confirmar("UPC_FAIL");

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
    }

    @Test
    @DisplayName("Confirmar con respuesta texto plano 'Applied'")
    void confirmar_PlanoApplied() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC_APP")
                .estado("PENDIENTE")
                .monto(new BigDecimal("100.00"))
                .build();
        when(transaccionRepository.findByUpc("UPC_APP")).thenReturn(Optional.of(tx));
        when(sentTxClient.confirmTx(anyString(), any(), any())).thenReturn("Applied");

        PagoServicioResponse resp = gateway.confirmar("UPC_APP");

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.APLICADO);
    }

    @Test
    @DisplayName("Confirmar con respuesta texto plano 'Not Applied'")
    void confirmar_PlanoNotApplied() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC_NOT_APP")
                .estado("PENDIENTE")
                .monto(new BigDecimal("100.00"))
                .build();
        when(transaccionRepository.findByUpc("UPC_NOT_APP")).thenReturn(Optional.of(tx));
        when(sentTxClient.confirmTx(anyString(), any(), any())).thenReturn("Not Applied");

        PagoServicioResponse resp = gateway.confirmar("UPC_NOT_APP");

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.RECHAZADO);
    }

    @Test
    @DisplayName("Confirmar con respuesta indeterminada")
    void confirmar_Indeterminada() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC_INDET")
                .estado("PENDIENTE")
                .monto(new BigDecimal("100.00"))
                .build();
        when(transaccionRepository.findByUpc("UPC_INDET")).thenReturn(Optional.of(tx));
        when(sentTxClient.confirmTx(anyString(), any(), any())).thenReturn("UNKNOWN_STATUS");

        PagoServicioResponse resp = gateway.confirmar("UPC_INDET");

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
    }

    @Test
    @DisplayName("Resolver token inexistente solicita renovación")
    void resolverToken_SinToken_RenuevaYObtiene() {
        GestoPagoToken nuevoToken = new GestoPagoToken();
        nuevoToken.setToken("nuevo_token_renovado");

        when(tokenService.obtenerTokenActivo(any(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(nuevoToken));
        when(upcGenerator.generarUpc()).thenReturn("UPC_RENOV");
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn("Applied");

        PagoServicioResponse resp = gateway.enviar(pago);

        verify(tokenService, times(1)).renovarToken();
        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Resolver token que sigue vacío tras renovar lanza IntegracionNoDisponibleException")
    void resolverToken_SigueVacio_LanzaExcepcion() {
        when(tokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.empty());

        assertThrows(IntegracionNoDisponibleException.class, () -> gateway.enviar(pago));
    }
}
