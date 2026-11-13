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

        GestoPagoToken token = new GestoPagoToken();
        token.setToken("valid_token");
        when(tokenService.obtenerTokenActivo(any(), any())).thenReturn(Optional.of(token));
    }

    @Test
    @DisplayName("Modo simulación exitosa al enviar pago")
    void enviar_ModoSimulacion() {
        ReflectionTestUtils.setField(gateway, "simulacionExitosa", true);
        when(upcGenerator.generarUpc()).thenReturn("UPC123");
        when(aesCipherService.cifrar(anyString())).thenReturn("cifrado");

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
        when(aesCipherService.cifrar(anyString())).thenReturn("cifrado");

        Request request = Request.create(Request.HttpMethod.POST, "/url", Collections.emptyMap(), null, new RequestTemplate());
        when(sentTxClient.sendTx(anyString(), any(), any())).thenThrow(new FeignException.Forbidden("Forbidden", request, null, null));

        assertThrows(IntegracionNoDisponibleException.class, () -> gateway.enviar(pago));
        verify(tokenService, times(1)).renovarToken();
    }

    @Test
    @DisplayName("Enviar cuando proveedor devuelve respuesta nula o vacía")
    void enviar_RespuestaVacia() {
        when(upcGenerator.generarUpc()).thenReturn("UPC_VACIO");
        when(aesCipherService.cifrar(anyString())).thenReturn("cifrado");
        when(sentTxClient.sendTx(anyString(), any(), any())).thenReturn("");

        PagoServicioResponse resp = gateway.enviar(pago);

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
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
        when(aesCipherService.cifrar(anyString())).thenReturn("cifrado");
        when(sentTxClient.confirmTx(anyString(), any(), any())).thenReturn("");

        PagoServicioResponse resp = gateway.confirmar("UPC_PEND");

        assertThat(resp.getEstado()).isEqualTo(EstadoPagoServicio.PENDIENTE_CONFIRMACION);
    }
}
