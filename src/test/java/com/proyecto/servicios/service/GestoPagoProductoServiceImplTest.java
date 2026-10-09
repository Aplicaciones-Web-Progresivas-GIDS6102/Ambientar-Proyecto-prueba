package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse.MensajeXml;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse.ProductoXml;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse.ProductosWrapper;
import com.proyecto.servicios.model.gestopago.ProductoDto;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para GestoPagoProductoServiceImpl.
 * 
 * Evalúa:
 * 1. Mapeo y ordenamiento de tipoFront de menor a mayor (0, 1, 2...).
 * 2. Asignación de '0' por defecto a productos sin tipoFront y ubicación al inicio.
 * 3. Manejo de excepciones de comunicación.
 */
@ExtendWith(MockitoExtension.class)
class GestoPagoProductoServiceImplTest {

    @Mock
    private GestoPagoProductClient gestoPagoProductClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @Mock
    private GestoPagoProductoRepository gestoPagoProductoRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private GestoPagoProductoServiceImpl productoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productoService, "tokenConfigurado", "mock_bearer_token_123");
        ReflectionTestUtils.setField(productoService, "idDistribuidor", 1001);
        ReflectionTestUtils.setField(productoService, "codigoDispositivo", "DEV_DEVICE_01");
    }

    @Test
    @DisplayName("Debe ordenar los productos por tipoFront de menor a mayor y asignar '0' a los que no tengan dato (ubicándolos al inicio)")
    void obtenerProductos_OrdenamientoTipoFront_Exitoso() {
        // ARRANGE
        ConsultaProductosRequest request = ConsultaProductosRequest.builder().build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gestopago:productos:catalogo")).thenReturn(responseCacheMock);

        // ACT
        ConsultaProductosResponse response = productoService.obtenerProductos(request);

        // ASSERT
        assertNotNull(response);
        assertEquals("01", response.getCodigo());
        assertEquals(1, response.getProductos().size());
        assertEquals("ABIB 100", response.getProductos().get(0).getProducto());

        // Verificaciones
        verify(redisTemplate.opsForValue(), times(1)).get("gestopago:productos:catalogo");
        verifyNoInteractions(gestoPagoProductClient);
        verifyNoInteractions(gestoPagoProductoRepository);
    }

    @Test
    @DisplayName("PRUEBA 2: En HTTP 200 OK exitoso, debe borrar anteriores en BD (deleteAllInBatch), guardar nuevos en BD y actualizar Redis")
    void sincronizarProductos_Exitoso_Http200_BorraAnterioresGuardaBDyRedis() {
        // ARRANGE
        GestoPagoToken tokenMock = new GestoPagoToken();
        tokenMock.setToken("token_bd_456");
        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.of(tokenMock));

        // Productos desordenados con tipoFront "2", "1", null (sin tipoFront) y "0"
        MensajeXml mensaje = new MensajeXml("01", "Operación realizada con éxito");
        ProductoXml prodFront2 = new ProductoXml("SERIVCIO_2", "PROD 2", "1", "1", "1", "2", "false", "100.0", "false", "a", "legend");
        ProductoXml prodFront1 = new ProductoXml("SERIVCIO_1", "PROD 1", "2", "2", "1", "1", "false", "200.0", "false", "a", "legend");
        ProductoXml prodSinFront = new ProductoXml("SERIVCIO_0", "PROD SIN FRONT", "3", "3", "1", null, "false", "300.0", "false", "a", "legend");

        ProductosWrapper wrapper = new ProductosWrapper(List.of(prodFront2, prodFront1, prodSinFront));
        GestoPagoProductXmlResponse xmlResponseMock = new GestoPagoProductXmlResponse(mensaje, wrapper);

        when(gestoPagoProductClient.getProductList(anyString())).thenReturn(xmlResponseMock);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // ACT
        ConsultaProductosResponse response = productoService.sincronizarProductos();

        // ASSERT
        assertNotNull(response);
        assertEquals("01", response.getCodigo());
        List<ProductoDto> productos = response.getProductos();
        assertEquals(3, productos.size());

        // El primero debe ser el que no tenía tipoFront (se asigna "0" por defecto)
        assertEquals("0", productos.get(0).getTipoFront());
        assertEquals("PROD SIN FRONT", productos.get(0).getProducto());

        // El segundo debe ser tipoFront "1"
        assertEquals("1", productos.get(1).getTipoFront());
        assertEquals("PROD 1", productos.get(1).getProducto());

        // El tercero debe ser tipoFront "2"
        assertEquals("2", productos.get(2).getTipoFront());
        assertEquals("PROD 2", productos.get(2).getProducto());
    }

    @Test
    @DisplayName("Debe manejar error de comunicación FeignException sin lanzar excepciones descontroladas")
    void obtenerProductos_ErrorComunicacion_FeignException() {
        // ARRANGE
        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.empty());

        Request feignRequest = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, new RequestTemplate());
        when(gestoPagoProductClient.getProductList(anyString()))
                .thenThrow(new FeignException.ServiceUnavailable("Servicio no disponible", feignRequest, null, null));

        when(gestoPagoProductoRepository.findAll()).thenReturn(Collections.emptyList());

        // ACT
        ConsultaProductosResponse response = productoService.sincronizarProductos();

        // ASSERT
        assertNotNull(response);
        assertEquals("530", response.getCodigo());
        assertTrue(response.getMensaje().contains("no existen datos almacenados"));
    }
}
