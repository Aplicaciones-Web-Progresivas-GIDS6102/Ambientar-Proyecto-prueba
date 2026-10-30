package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.dto.Solicitud2FARequestDTO;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import com.proyecto.servicios.handler.PagoServicioHandler;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GestoPagoPagoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PagoServicioHandler pagoServicioHandler;

    @InjectMocks
    private GestoPagoPagoController controller;

    private ObjectMapper objectMapper;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        objectMapper = new ObjectMapper();
        auth = new UsernamePasswordAuthenticationToken("carlos.user", null, Collections.emptyList());
    }

    @Test
    @DisplayName("Solicitud de 2FA exitosa: devuelve código interno 0 y mensaje de envío")
    void solicitar2FA_Exitoso() throws Exception {
        Solicitud2FARequestDTO req = Solicitud2FARequestDTO.builder()
                .numCuenta("4000123456")
                .idServicio("2284")
                .idProducto("14302")
                .monto(new BigDecimal("100.00"))
                .telefono("5512345678")
                .build();

        GenericResponse handlerResp = GenericResponse.builder()
                .codigo(0)
                .mensaje("Código de segunda autenticación enviado con éxito.")
                .data(Map.of("vigenciaSegundos", 300))
                .build();

        when(pagoServicioHandler.solicitar2FA(any(), eq("carlos.user"))).thenReturn(handlerResp);

        mockMvc.perform(post("/api/v1/gestopago/pagos/solicitar-2fa")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(0))
                .andExpect(jsonPath("$.mensaje").value("Código de segunda autenticación enviado con éxito."));
    }

    @Test
    @DisplayName("Pagar servicio exitoso: devuelve HTTP 200 con código interno 0")
    void pagarServicio_Exitoso_DevuelveCodigoCero() throws Exception {
        PagoServicioRequest req = PagoServicioRequest.builder()
                .numCuenta("4000123456")
                .idServicio("2284")
                .idProducto("14302")
                .monto(new BigDecimal("100.00"))
                .telefono("5512345678")
                .codigo2FA("123456")
                .build();

        PagoServicioResponse pagoResp = PagoServicioResponse.builder()
                .estado(EstadoPagoServicio.APLICADO)
                .upc("UPC123")
                .monto(new BigDecimal("100.00"))
                .mensaje("Pago aplicado exitosamente.")
                .build();

        GenericResponse genericResp = GenericResponse.builder()
                .codigo(0) // Código interno 0 requerido
                .mensaje("Pago aplicado exitosamente.")
                .data(pagoResp)
                .build();

        when(pagoServicioHandler.procesarPago(any(), eq("carlos.user")))
                .thenReturn(new ResponseEntity<>(genericResp, HttpStatus.OK));

        mockMvc.perform(post("/api/v1/gestopago/pagos")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(0))
                .andExpect(jsonPath("$.data.upc").value("UPC123"));
    }

    @Test
    @DisplayName("Pagar servicio sin autenticación: lanza AccessDeniedException (HTTP 403)")
    void pagarServicio_SinAutenticacion_Rechaza() throws Exception {
        PagoServicioRequest req = PagoServicioRequest.builder()
                .numCuenta("4000123456")
                .idServicio("2284")
                .idProducto("14302")
                .monto(new BigDecimal("100.00"))
                .telefono("5512345678")
                .codigo2FA("123456")
                .build();

        mockMvc.perform(post("/api/v1/gestopago/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    @DisplayName("Conciliar transacción exitosa: confirma y actualiza estado")
    void conciliarTransaccion_Exitoso() throws Exception {
        GenericResponse resp = GenericResponse.builder()
                .codigo(0)
                .mensaje("Resultado de conciliación: APLICADO")
                .build();

        when(pagoServicioHandler.conciliarTransaccion("UPC123")).thenReturn(resp);

        mockMvc.perform(post("/api/v1/gestopago/pagos/confirmar/UPC123")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(0))
                .andExpect(jsonPath("$.mensaje").value("Resultado de conciliación: APLICADO"));
    }
}
