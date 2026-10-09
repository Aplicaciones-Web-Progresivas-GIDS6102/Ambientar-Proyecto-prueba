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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementación de la capa de servicio para consultar y sincronizar productos de GestoPago.
 * 
 * ¿QUÉ HACE ESTE SERVICIO?
 * 1. CACHÉ EN REDIS: Almacena y recupera el catálogo de productos con la clave 'gestopago:productos:catalogo'.
 * 2. BORRADO CONDICIONAL EN BD (HTTP 200): Solo cuando el proveedor responde exitosamente (HTTP 200 OK),
 *    elimina los productos anteriores en PostgreSQL (`deleteAllInBatch()`) para evitar duplicados e inserta la nueva lista.
 * 3. RESPALDO OFFLINE: Si la llamada externa falla (sin Internet), recupera los datos almacenados en PostgreSQL.
 * 4. ORDENAMIENTO POR STREAM Y PREDICATE: Ordena los productos de menor a mayor según el atributo 'tipoFront' (0, 1, 2, ...).
 *    Si un producto no incluye 'tipoFront', se le asigna por defecto "0" y se coloca AL INICIO del resultado.
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
     * Consulta los productos. Primero revisa Redis Caché; si no está en caché, sincroniza.
     */
    @Override
    public ConsultaProductosResponse obtenerProductos(ConsultaProductosRequest request) {
        log.info("Iniciando consulta de productos GestoPago...");

        try {
            Object cachedData = redisTemplate.opsForValue().get(REDIS_CACHE_KEY);
            if (cachedData instanceof ConsultaProductosResponse responseCache) {
                log.info("Catálogo de productos recuperado desde CACHÉ REDIS (Cache Hit). Total productos: {}",
                        responseCache.getProductos() != null ? responseCache.getProductos().size() : 0);
                return responseCache;
            }
        } catch (Exception e) {
            log.warn("No se pudo consultar Redis Caché: {}. Procediendo a consulta directa.", e.getMessage());
        }

        return sincronizarProductos();
    }

    /**
     * Sincroniza productos desde la API externa, borra anteriores en BD en HTTP 200 OK,
     * ordena los productos por 'tipoFront' (de menor a mayor, por defecto 0) usando Streams y guarda en Redis/BD.
     */
    @Override
    @Transactional
    public ConsultaProductosResponse sincronizarProductos() {
        log.info("Iniciando proceso de sincronización de productos GestoPago...");

        String rawToken = resolverBearerToken();
        String authorizationHeader = formatearBearerHeader(rawToken);

        log.info("Consumiendo getProductList.do con token enmascarado: {}", enmascararToken(rawToken));

        try {
            GestoPagoProductXmlResponse xmlResponse = gestoPagoProductClient.getProductList(authorizationHeader);

            if (xmlResponse == null) {
                log.warn("El proveedor GestoPago devolvió una respuesta nula. Recurriendo a respaldo en BD.");
                return recuperarDesdeBaseDeDatosOffline("Proveedor devolvió respuesta nula.");
            }

            // 1. Mapear productos del XML
            List<ProductoDto> dtoList = mapearProductosDto(xmlResponse);

            // 2. ORDENAR PRODUCTOS POR TIPO FRONT DE MENOR A MAYOR USANDO STREAMS (SIN TIPO FRONT -> DEFAULT "0" AL INICIO)
            dtoList = ordenarProductosPorTipoFront(dtoList);

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

            // CONDICIÓN: Si responde HTTP 200 OK (Código "01"), borrar anteriores en BD y guardar la nueva lista ordenada
            if ("01".equals(codigoNegocio)) {
                log.info("Respuesta HTTP 200 OK exitosa del proveedor. Eliminando registros anteriores en BD para evitar duplicados...");

                gestoPagoProductoRepository.deleteAllInBatch();
                log.info("Registros anteriores eliminados de la base de datos.");

                List<GestoPagoProducto> entidadesParaGuardar = mapearEntidadesBd(dtoList);
                if (!entidadesParaGuardar.isEmpty()) {
                    gestoPagoProductoRepository.saveAll(entidadesParaGuardar);
                    log.info("Se guardaron {} productos ordenados por tipoFront en PostgreSQL.", entidadesParaGuardar.size());
                }

                guardarEnRedis(responseSuccess);
            }

            return responseSuccess;

        } catch (FeignException e) {
            log.error("Error de comunicación HTTP con GestoPago (Status: {}): {}. Recurriendo a respaldo de BD...", e.status(), e.getMessage());
            return recuperarDesdeBaseDeDatosOffline("Error de red/comunicación con el proveedor externo.");

        } catch (Exception e) {
            log.error("Excepción inesperada en la sincronización: {}. Recurriendo a respaldo de BD...", e.getMessage(), e);
            return recuperarDesdeBaseDeDatosOffline("Falla inesperada en la sincronización.");
        }
    }

    /**
     * Ordena la lista de productos por el atributo 'tipoFront' de menor a mayor utilizando Java Streams, Predicates y Comparators.
     * 
     * ¿QUÉ HACE?
     * Revisa cada producto. Si no contiene 'tipoFront' (es nulo o vacío), asigna por defecto "0"
     * y posiciona esos productos al INICIO del resultado (orden ascendente: 0, 1, 2, ...).
     * 
     * ¿CÓMO LO HACE?
     * 1. Define un Predicate para detectar elementos sin tipoFront.
     * 2. Usa `stream().map(...)` para normalizar los valores faltantes a "0".
     * 3. Usa `sorted(Comparator.comparingInt(...))` para realizar el ordenamiento numérico de menor a mayor.
     * 4. Retorna la lista ordenada mediante `collect(Collectors.toList())`.
     */
    private List<ProductoDto> ordenarProductosPorTipoFront(List<ProductoDto> lista) {
        if (lista == null || lista.isEmpty()) {
            return new ArrayList<>();
        }

        // Predicado funcional para identificar elementos que carecen del atributo tipoFront
        Predicate<ProductoDto> sinTipoFront = dto -> dto.getTipoFront() == null || dto.getTipoFront().isBlank();

        return lista.stream()
                .map(dto -> {
                    // Si no se tiene dato en el atributo tipoFront, se toma como "0"
                    if (sinTipoFront.test(dto)) {
                        dto.setTipoFront("0");
                    } else {
                        dto.setTipoFront(dto.getTipoFront().trim());
                    }
                    return dto;
                })
                // Ordenamiento numérico ascendente (de menor a mayor): 0 -> 1 -> 2 ...
                .sorted(Comparator.comparingInt(this::convertirTipoFrontAEntero))
                .collect(Collectors.toList());
    }

    /**
     * Convierte la cadena 'tipoFront' a su equivalente entero para la comparación del Stream.
     * Retorna 0 si es nulo, en blanco o no numérico.
     */
    private int convertirTipoFrontAEntero(ProductoDto dto) {
        if (dto == null || dto.getTipoFront() == null || dto.getTipoFront().isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(dto.getTipoFront().trim());
        } catch (NumberFormatException e) {
            return 0; // Si no es numérico, se ubica al inicio junto con los "0"
        }
    }

    /**
     * Modo Offline: Recupera productos de PostgreSQL y los retorna ordenados por 'tipoFront'.
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
                        .tipoFront(entity.getTipoFront() != null ? entity.getTipoFront() : "0")
                        .hasDigitoVerificador(entity.getHasDigitoVerificador() != null && entity.getHasDigitoVerificador())
                        .precio(entity.getPrecio() != null ? entity.getPrecio() : 0.0)
                        .showAyuda(entity.getShowAyuda() != null && entity.getShowAyuda())
                        .tipoReferencia(entity.getTipoReferencia() != null ? entity.getTipoReferencia() : "")
                        .legend(entity.getLegend() != null ? entity.getLegend() : "")
                        .build());
            }

            // Aplicar ordenamiento por Stream también al recuperar desde BD offline
            dtoList = ordenarProductosPorTipoFront(dtoList);

            log.info("MODO OFFLINE EXITOSO: Se recuperaron y ordenaron {} productos desde la BD PostgreSQL.", dtoList.size());

            return ConsultaProductosResponse.builder()
                    .codigo("01")
                    .mensaje("Productos recuperados desde Base de Datos local (Modo Offline). Causa: " + causaFalla)
                    .productos(dtoList)
                    .build();

        } catch (Exception e) {
            log.error("Error al consultar la Base de Datos de respaldo: {}", e.getMessage(), e);
            return ConsultaProductosResponse.builder()
                    .codigo("500")
                    .mensaje("Error al acceder a la base de datos de respaldo offline.")
                    .productos(new ArrayList<>())
                    .build();
        }
    }

    private void guardarEnRedis(ConsultaProductosResponse response) {
        try {
            redisTemplate.opsForValue().set(REDIS_CACHE_KEY, response, Duration.ofHours(24));
            log.info("Catálogo de productos ordenado actualizado exitosamente en REDIS CACHÉ (TTL: 24 horas).");
        } catch (Exception e) {
            log.warn("No se pudo guardar la información en Redis Caché: {}", e.getMessage());
        }
    }

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

    private String formatearBearerHeader(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (token.startsWith("Bearer ")) {
            return token;
        }
        return "Bearer " + token;
    }

    private String enmascararToken(String token) {
        if (token == null || token.length() < 10) {
            return "****";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

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

            String tipoFrontValor = item.getTipoFront() != null && !item.getTipoFront().isBlank() ? item.getTipoFront() : "0";

            ProductoDto dto = ProductoDto.builder()
                    .servicio(item.getServicio() != null ? item.getServicio() : "")
                    .producto(item.getProducto() != null ? item.getProducto() : "")
                    .idServicio(item.getIdServicio() != null ? item.getIdServicio() : "")
                    .idProducto(item.getIdProducto() != null ? item.getIdProducto() : "")
                    .idCatTipoServicio(item.getIdCatTipoServicio() != null ? item.getIdCatTipoServicio() : "")
                    .tipoFront(tipoFrontValor)
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
