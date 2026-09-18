package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse.MensajeXml;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse.ProductoXml;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse.ProductosWrapper;
import com.proyecto.servicios.service.Impl.GestoPagoProductoServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para la clase de servicio GestoPagoProductoServiceImpl.
 * 
 * Evalúa los escenarios de:
 * 1. Respuesta exitosa con mapeo de productos.
 * 2. Manejo de error de comunicación o timeout (FeignException).
 * 3. Manejo de error de autenticación 401.
 */
@ExtendWith(MockitoExtension.class)
class GestoPagoProductoServiceImplTest {

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @InjectMocks
    private GestoPagoProductoServiceImpl productoService;

    @BeforeEach
    void setUp() {
        // Inyectar valor por defecto en la propiedad tokenConfigurado mediante ReflectionTestUtils
        ReflectionTestUtils.setField(productoService, "tokenConfigurado", "mock_bearer_token_123");
        ReflectionTestUtils.setField(productoService, "idDistribuidor", 1001);
        ReflectionTestUtils.setField(productoService, "codigoDispositivo", "DEV_DEVICE_01");
    }

    @Test
    @DisplayName("Debe obtener la lista de productos exitosamente y mapear correctamente desde el XML")
    void obtenerProductos_Exitoso() {
        // ARRANGE
        ConsultaProductosRequest request = ConsultaProductosRequest.builder()
                .usuario("usuarioTest")
                .password("password123")
                .build();

        GestoPagoToken tokenMock = new GestoPagoToken();
        tokenMock.setToken("token_bd_456");
        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.of(tokenMock));

        // Preparación del XML simulado devuelto por el cliente externo
        MensajeXml mensaje = new MensajeXml("01", "Operación realizada con éxito");
        ProductoXml prod1 = new ProductoXml("ABIB", "ABIB 100", "2284", "14302", "13", "1", "false", "100.0", "false", "a", "Soporte 24h");
        ProductosWrapper wrapper = new ProductosWrapper(List.of(prod1));
        GestoPagoProductXmlResponse xmlResponseMock = new GestoPagoProductXmlResponse(mensaje, wrapper);

        when(gestoPagoProductClient.getProductList(anyString())).thenReturn(xmlResponseMock);

        // ACT
        ConsultaProductosResponse response = productoService.obtenerProductos(request);

        // ASSERT
        assertNotNull(response, "La respuesta no debe ser nula");
        assertEquals("01", response.getCodigo(), "El código de respuesta debe ser 01");
        assertEquals("Operación realizada con éxito", response.getMensaje());
        assertNotNull(response.getProductos(), "La lista de productos no debe ser nula");
        assertEquals(1, response.getProductos().size(), "Debe existir 1 producto mapeado");

        assertEquals("ABIB", response.getProductos().get(0).getServicio());
        assertEquals("ABIB 100", response.getProductos().get(0).getProducto());
        assertEquals(100.0, response.getProductos().get(0).getPrecio());
        assertFalse(response.getProductos().get(0).getHasDigitoVerificador());

        verify(gestoPagoProductClient, times(1)).getProductList("Bearer token_bd_456");
    }

    @Test
    @DisplayName("Debe manejar gracefully un error de comunicación FeignException sin devolver nulos")
    void obtenerProductos_ErrorComunicacion_FeignException() {
        // ARRANGE
        ConsultaProductosRequest request = ConsultaProductosRequest.builder()
                .usuario("usuarioTest")
                .password("password123")
                .build();

        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.empty());

        Request feignRequest = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, new RequestTemplate());
        when(gestoPagoProductClient.getProductList(anyString()))
                .thenThrow(new FeignException.ServiceUnavailable("Servicio no disponible", feignRequest, null, null));

        // ACT
        ConsultaProductosResponse response = productoService.obtenerProductos(request);

        // ASSERT
        assertNotNull(response);
        assertEquals("503", response.getCodigo());
        assertTrue(response.getMensaje().contains("Error de comunicación"));
        assertNotNull(response.getProductos());
        assertTrue(response.getProductos().isEmpty());
    }

    @Test
    @DisplayName("Debe manejar error 401 Unauthorized devolviendo respuesta estandarizada")
    void obtenerProductos_ErrorAutenticacion_Unauthorized() {
        // ARRANGE
        ConsultaProductosRequest request = ConsultaProductosRequest.builder()
                .usuario("usuarioTest")
                .password("password123")
                .build();

        Request feignRequest = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, new RequestTemplate());
        when(gestoPagoProductClient.getProductList(anyString()))
                .thenThrow(new FeignException.Unauthorized("Token inválido", feignRequest, null, null));

        // ACT
        ConsultaProductosResponse response = productoService.obtenerProductos(request);

        // ASSERT
        assertNotNull(response);
        assertEquals("401", response.getCodigo());
        assertTrue(response.getMensaje().contains("autenticación"));
        assertNotNull(response.getProductos());
        assertTrue(response.getProductos().isEmpty());
    }
}
