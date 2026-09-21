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
 * Pruebas unitarias completas para GestoPagoProductoServiceImpl.
 * 
 * Evalúa las nuevas funcionalidades:
 * 1. Caché Hit en Redis.
 * 2. Borrado masivo condicional en BD (deleteAllInBatch) únicamente tras HTTP 200 OK.
 * 3. Persistencia de nuevos productos en PostgreSQL y guardado en Redis con TTL.
 * 4. Tolerancia a fallos y modo offline desde BD cuando falla el servicio externo (sin Internet).
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
    @DisplayName("PRUEBA 1: Debe retornar productos directamente desde Redis Caché cuando existe (Cache Hit) sin llamar a Feign o BD")
    void obtenerProductos_CacheHit_RetornaDesdeRedis() {
        // ARRANGE
        ConsultaProductosRequest request = ConsultaProductosRequest.builder().build();
        ConsultaProductosResponse responseCacheMock = ConsultaProductosResponse.builder()
                .codigo("01")
                .mensaje("Operacion realizada con exito")
                .productos(List.of(ProductoDto.builder().servicio("ABIB").producto("ABIB 100").precio(100.0).build()))
                .build();

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

        MensajeXml mensaje = new MensajeXml("01", "Operación realizada con éxito");
        ProductoXml prod1 = new ProductoXml("ABIB", "ABIB 100", "2284", "14302", "13", "1", "false", "100.0", "false", "a", "Soporte 24h");
        ProductosWrapper wrapper = new ProductosWrapper(List.of(prod1));
        GestoPagoProductXmlResponse xmlResponseMock = new GestoPagoProductXmlResponse(mensaje, wrapper);

        when(gestoPagoProductClient.getProductList(anyString())).thenReturn(xmlResponseMock);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // ACT
        ConsultaProductosResponse response = productoService.sincronizarProductos();

        // ASSERT
        assertNotNull(response);
        assertEquals("01", response.getCodigo());
        assertEquals(1, response.getProductos().size());

        // VERIFICACIONES CLAVE:
        // 1. Debe haber llamado a deleteAllInBatch() para eliminar los anteriores y evitar duplicados
        verify(gestoPagoProductoRepository, times(1)).deleteAllInBatch();
        // 2. Debe haber guardado los nuevos productos en PostgreSQL
        verify(gestoPagoProductoRepository, times(1)).saveAll(any());
        // 3. Debe haber guardado en Redis con un TTL de 24 horas
        verify(valueOperations, times(1)).set(eq("gestopago:productos:catalogo"), any(ConsultaProductosResponse.class), eq(Duration.ofHours(24)));
    }

    @Test
    @DisplayName("PRUEBA 3: Cuando falla el servicio externo (Sin Internet), NO debe borrar la BD y debe recuperar de BD (Modo Offline)")
    void sincronizarProductos_SinInternet_FallaServicio_RecuperaDeBaseDeDatosOffline() {
        // ARRANGE
        when(gestoPagoTokenService.obtenerTokenActivo(anyInt(), anyString())).thenReturn(Optional.empty());

        Request feignRequest = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, new RequestTemplate());
        when(gestoPagoProductClient.getProductList(anyString()))
                .thenThrow(new FeignException.ServiceUnavailable("Servicio no disponible", feignRequest, null, null));

        GestoPagoProducto entidadBdMock = GestoPagoProducto.builder()
                .id(1L)
                .servicio("ABIB")
                .producto("ABIB 150 (Offline)")
                .precio(150.0)
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(gestoPagoProductoRepository.findAll()).thenReturn(List.of(entidadBdMock));

        // ACT
        ConsultaProductosResponse response = productoService.sincronizarProductos();

        // ASSERT
        assertNotNull(response);
        assertEquals("01", response.getCodigo());
        assertTrue(response.getMensaje().contains("Modo Offline"));
        assertEquals(1, response.getProductos().size());
        assertEquals("ABIB 150 (Offline)", response.getProductos().get(0).getProducto());

        // VERIFICACIONES CLAVE:
        // 1. NO se debe haber ejecutado borrado en la base de datos
        verify(gestoPagoProductoRepository, never()).deleteAllInBatch();
        // 2. Se debieron consultar los registros de la BD
        verify(gestoPagoProductoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("PRUEBA 4: Si falla el servicio externo y la BD también está vacía, debe retornar error 530 gracefully")
    void sincronizarProductos_SinInternet_y_BDEmpty_RetornaError530() {
        // ARRANGE
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
        assertTrue(response.getProductos().isEmpty());
    }
}
