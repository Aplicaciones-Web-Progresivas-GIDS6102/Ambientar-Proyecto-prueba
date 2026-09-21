package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse.ProductoXml;
import com.proyecto.servicios.model.gestopago.ProductoDto;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.GestoPagoProductoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de la capa de servicio para consultar y sincronizar productos de GestoPago.
 * 
 * ¿QUÉ HACE ESTE SERVICIO?
 * Maneja la estrategia de caché en Redis, sincronización con el proveedor externo GestoPago,
 * actualización limpia de la base de datos PostgreSQL en respuestas HTTP 200 y respaldo en modo offline cuando no hay Internet.
 * 
 * ¿CÓMO LO HACE?
 * 1. CACHÉ EN REDIS: Al consultar productos, revisa primero la clave 'gestopago:productos:catalogo' en Redis para rápida respuesta.
 * 2. RESPUESTA HTTP 200 DEL PROVEEDOR: Invocado por el Cron (6:00 AM) o por falta de caché, llama al cliente Feign.
 *    Si la respuesta es exitosa (HTTP 200):
 *    a) Ejecuta `gestoPagoProductoRepository.deleteAllInBatch()` para ELIMINAR los productos anteriores en la BD PostgreSQL (evitando duplicados).
 *    b) Convierte los datos y guarda la nueva lista mediante `saveAll()`.
 *    c) Guarda la respuesta en Redis con un tiempo de vida (TTL) de 24 horas.
 * 3. MODO OFFLINE / CAÍDA DE RED (SIN INTERNET): Si la llamada externa falla (FeignException, Timeout, etc.):
 *    a) No elimina nada de la BD.
 *    b) Consulta los registros previamente almacenados en PostgreSQL (`findAll()`).
 *    c) Devuelve la lista recuperada de la BD indicando en el mensaje que se sirve desde el respaldo offline.
 */
@Service
@Slf4j
public class GestoPagoProductoServiceImpl implements GestoPagoProductoService {

    private static final String REDIS_CACHE_KEY = "gestopago:productos:catalogo";

    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final GestoPagoProductoRepository gestoPagoProductoRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${gestopago.products.bearer-token:}")
    private String tokenConfigurado;

    @Value("${gestopago.auth.id-distribuidor:1001}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:DEV_DEVICE_01}")
    private String codigoDispositivo;

    /**
     * Inyección de dependencias por constructor.
     * 
     * ¿QUÉ HACE? Garantiza la inmutabilidad y facilita las pruebas unitarias del servicio.
     * ¿CÓMO LO HACE? Spring inyecta automáticamente las instancias del cliente Feign, repositorio, token service y RedisTemplate.
     */
    public GestoPagoProductoServiceImpl(GestoPagoProductClient gestoPagoProductClient,
                                        GestoPagoTokenService gestoPagoTokenService,
                                        GestoPagoProductoRepository gestoPagoProductoRepository,
                                        RedisTemplate<String, Object> redisTemplate) {
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.gestoPagoProductoRepository = gestoPagoProductoRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Consulta la lista de productos de GestoPago.
     * 
     * ¿QUÉ HACE?
     * Retorna los productos de forma rápida. Intenta leer desde Redis; si no existen en memoria,
     * ejecuta la sincronización con el servicio externo o base de datos.
     * 
     * ¿CÓMO LO HACE?
     * Revisa si existe el catálogo en Redis mediante `redisTemplate.opsForValue().get(...)`.
     * - Si existe: Devuelve el objeto deserializado de Redis (Cache Hit).
     * - Si no existe (Cache Miss): Ejecuta `sincronizarProductos()`.
     */
    @Override
    public ConsultaProductosResponse obtenerProductos(ConsultaProductosRequest request) {
        log.info("Iniciando consulta de productos GestoPago...");

        try {
            // 1. Intentar obtener desde la memoria caché Redis
            Object cachedData = redisTemplate.opsForValue().get(REDIS_CACHE_KEY);
            if (cachedData instanceof ConsultaProductosResponse responseCache) {
                log.info("Catálogo de productos recuperado exitosamente desde CACHÉ REDIS (Cache Hit). Total productos: {}",
                        responseCache.getProductos() != null ? responseCache.getProductos().size() : 0);
                return responseCache;
            }
        } catch (Exception e) {
            log.warn("No se pudo consultar Redis Caché (servidor Redis inalcanzable o no configurado): {}. Procediendo a consulta directa.", e.getMessage());
        }

        // 2. Si no estaba en caché o Redis falló, sincronizar y obtener productos
        return sincronizarProductos();
    }

    /**
     * Sincroniza la lista de productos desde el proveedor externo y gestiona el almacenamiento en Redis y BD.
     * 
     * ¿QUÉ HACE?
     * Consume el servicio web externo de GestoPago. Si responde HTTP 200, limpia la BD PostgreSQL y guarda los nuevos datos,
     * actualizando también la caché en Redis. Si la conexión falla (sin Internet), recurre a los datos de la BD.
     * 
     * ¿CÓMO LO HACE?
     * 1. Resuelve el Bearer Token activo y llama a Feign `getProductList`.
     * 2. Si responde OK (HTTP 200):
     *    - Aplica `@Transactional` para ejecutar `gestoPagoProductoRepository.deleteAllInBatch()`, eliminando los productos viejos.
     *    - Mapea los datos a entidades y ejecuta `saveAll()` en PostgreSQL.
     *    - Guarda la respuesta en Redis con TTL de 24 horas (`Duration.ofHours(24)`).
     * 3. Si ocurre una excepción (Caída de red, FeignException):
     *    - Captura el error y llama a `recuperarDesdeBaseDeDatosOffline()`.
     */
    @Override
    @Transactional
    public ConsultaProductosResponse sincronizarProductos() {
        log.info("Iniciando proceso de sincronización con el proveedor externo GestoPago...");

        String rawToken = resolverBearerToken();
        String authorizationHeader = formatearBearerHeader(rawToken);

        log.info("Consumiendo getProductList.do con token enmascarado: {}", enmascararToken(rawToken));

        try {
            // Invocación Feign al endpoint externo
            GestoPagoProductXmlResponse xmlResponse = gestoPagoProductClient.getProductList(authorizationHeader);

            if (xmlResponse == null) {
                log.warn("El proveedor GestoPago devolvió una respuesta nula. Intentando respaldo en BD.");
                return recuperarDesdeBaseDeDatosOffline("Proveedor devolvió respuesta nula.");
            }

            List<ProductoDto> dtoList = mapearProductosDto(xmlResponse);

            // Determinar código y mensaje del proveedor
            String codigoNegocio = "01";
            String mensajeNegocio = "Operacion realizada con exito";
            if (xmlResponse.getMensaje() != null) {
                if (xmlResponse.getMensaje().getCodigo() != null) {
                    codigoNegocio = xmlResponse.getMensaje().getCodigo();
                }
                if (xmlResponse.getMensaje().getTexto() != null) {
                    mensajeNegocio = xmlResponse.getMensaje().getTexto();
                }
            }

            ConsultaProductosResponse responseSuccess = ConsultaProductosResponse.builder()
                    .codigo(codigoNegocio)
                    .mensaje(mensajeNegocio)
                    .productos(dtoList)
                    .build();

            // CONDICIÓN PRINCIPAL: Solo si responde con código exitoso ("01" / HTTP 200 OK)
            if ("01".equals(codigoNegocio)) {
                log.info("Respuesta HTTP 200 exitosa del proveedor. Procediendo a eliminar registros anteriores en BD para evitar duplicados...");

                // a) ELIMINAR REGISTROS ANTERIORES EN POSTGRESQL (Solo en HTTP 200 exitoso)
                gestoPagoProductoRepository.deleteAllInBatch();
                log.info("Registros anteriores eliminados correctamente de la base de datos.");

                // b) GUARDAR NUEVOS PRODUCTOS EN POSTGRESQL (Respaldo offline)
                List<GestoPagoProducto> entidadesParaGuardar = mapearEntidadesBd(dtoList);
                if (!entidadesParaGuardar.isEmpty()) {
                    gestoPagoProductoRepository.saveAll(entidadesParaGuardar);
                    log.info("Se guardaron {} nuevos productos en PostgreSQL para soporte offline.", entidadesParaGuardar.size());
                }

                // c) GUARDAR / ACTUALIZAR EN REDIS CACHÉ CON TTL DE 24 HORAS
                guardarEnRedis(responseSuccess);
            }

            return responseSuccess;

        } catch (FeignException e) {
            log.error("Error de comunicación HTTP con GestoPago (Status: {}): {}. Recurriendo a respaldo de BD...",
                    e.status(), e.getMessage());
            return recuperarDesdeBaseDeDatosOffline("Error de red/comunicación con el proveedor externo.");

        } catch (Exception e) {
            log.error("Excepción inesperada durante la sincronización: {}. Recurriendo a respaldo de BD...", e.getMessage(), e);
            return recuperarDesdeBaseDeDatosOffline("Falla inesperada en la sincronización.");
        }
    }

    /**
     * Recupera los productos respaldados en la base de datos PostgreSQL cuando el servicio externo está caído (sin Internet).
     * 
     * ¿QUÉ HACE?
     * Actúa como mecanismo de tolerancia a fallos (Fallback Mode Offline).
     * 
     * ¿CÓMO LO HACE?
     * Consulta `gestoPagoProductoRepository.findAll()`. Si existen productos guardados previamente, los convierte a DTOs
     * y los retorna al cliente con un mensaje informativo de operación en modo offline.
     */
    private ConsultaProductosResponse recuperarDesdeBaseDeDatosOffline(String causaFalla) {
        log.info("EJECUTANDO MODO OFFLINE: Consultando productos almacenados en la Base de Datos PostgreSQL...");

        try {
            List<GestoPagoProducto> productosBd = gestoPagoProductoRepository.findAll();

            if (productosBd.isEmpty()) {
                log.warn("La Base de Datos PostgreSQL tampoco contiene productos almacenados.");
                return ConsultaProductosResponse.builder()
                        .codigo("530")
                        .mensaje("Servicio externo no disponible y no existen datos almacenados en base de datos.")
                        .productos(new ArrayList<>())
                        .build();
            }

            List<ProductoDto> dtoList = new ArrayList<>();
            for (GestoPagoProducto entity : productosBd) {
                dtoList.add(ProductoDto.builder()
                        .servicio(entity.getServicio() != null ? entity.getServicio() : "")
                        .producto(entity.getProducto() != null ? entity.getProducto() : "")
                        .idServicio(entity.getIdServicio() != null ? entity.getIdServicio() : "")
                        .idProducto(entity.getIdProducto() != null ? entity.getIdProducto() : "")
                        .idCatTipoServicio(entity.getIdCatTipoServicio() != null ? entity.getIdCatTipoServicio() : "")
                        .tipoFront(entity.getTipoFront() != null ? entity.getTipoFront() : "")
                        .hasDigitoVerificador(entity.getHasDigitoVerificador() != null && entity.getHasDigitoVerificador())
                        .precio(entity.getPrecio() != null ? entity.getPrecio() : 0.0)
                        .showAyuda(entity.getShowAyuda() != null && entity.getShowAyuda())
                        .tipoReferencia(entity.getTipoReferencia() != null ? entity.getTipoReferencia() : "")
                        .legend(entity.getLegend() != null ? entity.getLegend() : "")
                        .build());
            }

            log.info("MODO OFFLINE EXITOSO: Se recuperaron {} productos desde la Base de Datos PostgreSQL.", dtoList.size());

            return ConsultaProductosResponse.builder()
                    .codigo("01")
                    .mensaje("Productos recuperados desde Base de Datos local (Modo Offline / Sin conexión). Causa: " + causaFalla)
                    .productos(dtoList)
                    .build();

        } catch (Exception e) {
            log.error("Error crítico al intentar consultar la Base de Datos de respaldo: {}", e.getMessage(), e);
            return ConsultaProductosResponse.builder()
                    .codigo("500")
                    .mensaje("Error al acceder a la base de datos de respaldo offline.")
                    .productos(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Almacena el objeto de respuesta en Redis Caché con tiempo de expiración (TTL).
     * 
     * ¿QUÉ HACE? Guarda la estructura JSON en la clave de Redis 'gestopago:productos:catalogo'.
     * ¿CÓMO LO HACE? Utiliza `redisTemplate.opsForValue().set(key, value, duration)`.
     */
    private void guardarEnRedis(ConsultaProductosResponse response) {
        try {
            redisTemplate.opsForValue().set(REDIS_CACHE_KEY, response, Duration.ofHours(24));
            log.info("Catálogo de productos actualizado exitosamente en REDIS CACHÉ (TTL: 24 horas).");
        } catch (Exception e) {
            log.warn("No se pudo guardar la información en Redis Caché: {}", e.getMessage());
        }
    }

    /**
     * Resuelve el Bearer Token dinámicamente.
     * 
     * ¿QUÉ HACE? Obtiene la credencial válida para autenticar contra GestoPago.
     * ¿CÓMO LO HACE? Consulta `GestoPagoTokenService` en BD o usa el token estático de `application.properties` como fallback.
     */
    private String resolverBearerToken() {
        try {
            Optional<GestoPagoToken> tokenOpt = gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
            if (tokenOpt.isPresent() && tokenOpt.get().getToken() != null && !tokenOpt.get().getToken().isBlank()) {
                return tokenOpt.get().getToken();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el token dinámico de BD/Service, utilizando token configurado: {}", e.getMessage());
        }
        return tokenConfigurado != null ? tokenConfigurado : "";
    }

    /**
     * Formatea el encabezado de autorización HTTP agregando el prefijo 'Bearer '.
     */
    private String formatearBearerHeader(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (token.startsWith("Bearer ")) {
            return token;
        }
        return "Bearer " + token;
    }

    /**
     * Enmascara tokens en logs para seguridad.
     */
    private String enmascararToken(String token) {
        if (token == null || token.length() < 10) {
            return "****";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    /**
     * Convierte la respuesta XML de GestoPago a objetos DTO limpios sin nulos.
     */
    private List<ProductoDto> mapearProductosDto(GestoPagoProductXmlResponse xmlResponse) {
        List<ProductoDto> dtoList = new ArrayList<>();
        if (xmlResponse.getProductos() == null || xmlResponse.getProductos().getListaProductos() == null) {
            return dtoList;
        }

        for (ProductoXml item : xmlResponse.getProductos().getListaProductos()) {
            if (item == null) continue;

            double precioParsed = 0.0;
            if (item.getPrecio() != null) {
                try {
                    precioParsed = Double.parseDouble(item.getPrecio());
                } catch (NumberFormatException ignored) {}
            }

            ProductoDto dto = ProductoDto.builder()
                    .servicio(item.getServicio() != null ? item.getServicio() : "")
                    .producto(item.getProducto() != null ? item.getProducto() : "")
                    .idServicio(item.getIdServicio() != null ? item.getIdServicio() : "")
                    .idProducto(item.getIdProducto() != null ? item.getIdProducto() : "")
                    .idCatTipoServicio(item.getIdCatTipoServicio() != null ? item.getIdCatTipoServicio() : "")
                    .tipoFront(item.getTipoFront() != null ? item.getTipoFront() : "")
                    .hasDigitoVerificador(Boolean.parseBoolean(item.getHasDigitoVerificador()))
                    .precio(precioParsed)
                    .showAyuda(Boolean.parseBoolean(item.getShowAyuda()))
                    .tipoReferencia(item.getTipoReferencia() != null ? item.getTipoReferencia() : "")
                    .legend(item.getLegend() != null ? item.getLegend() : "")
                    .build();

            dtoList.add(dto);
        }
        return dtoList;
    }

    /**
     * Convierte la lista de DTOs a entidades JPA `GestoPagoProducto` para su inserción en PostgreSQL.
     */
    private List<GestoPagoProducto> mapearEntidadesBd(List<ProductoDto> dtoList) {
        List<GestoPagoProducto> entidades = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();

        for (ProductoDto dto : dtoList) {
            entidades.add(GestoPagoProducto.builder()
                    .servicio(dto.getServicio())
                    .producto(dto.getProducto())
                    .idServicio(dto.getIdServicio())
                    .idProducto(dto.getIdProducto())
                    .idCatTipoServicio(dto.getIdCatTipoServicio())
                    .tipoFront(dto.getTipoFront())
                    .hasDigitoVerificador(dto.getHasDigitoVerificador())
                    .precio(dto.getPrecio())
                    .showAyuda(dto.getShowAyuda())
                    .tipoReferencia(dto.getTipoReferencia())
                    .legend(dto.getLegend())
                    .fechaActualizacion(ahora)
                    .build());
        }
        return entidades;
    }
}
