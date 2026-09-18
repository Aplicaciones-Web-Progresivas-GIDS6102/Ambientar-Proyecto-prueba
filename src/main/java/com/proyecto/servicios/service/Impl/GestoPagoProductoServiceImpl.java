package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse.ProductoXml;
import com.proyecto.servicios.model.gestopago.ProductoDto;
import com.proyecto.servicios.service.GestoPagoProductoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de la capa de servicio de negocio para consultar productos de GestoPago.
 * 
 * Principios aplicados:
 * - Clean Code e Inyección de Dependencias por Constructor.
 * - Registro en logs (SLF4J) al inicio y fin de la invocación sin exponer tokens sensibles.
 * - Manejo de excepciones (comunicación, timeout, autenticación) evitando retornos nulos.
 * - Mapeo defensivo de datos para garantizar cero valores nulos hacia la respuesta JSON.
 */
@Service
@Slf4j
public class GestoPagoProductoServiceImpl implements GestoPagoProductoService {

    private final GestoPagoProductClient gestoPagoProductClient;
    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.products.bearer-token:}")
    private String tokenConfigurado;

    @Value("${gestopago.auth.id-distribuidor:1001}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:DEV_DEVICE_01}")
    private String codigoDispositivo;


     //Inyección de dependencias mediante constructor para promover la inmutabilidad y facilitar pruebas unitarias.

    public GestoPagoProductoServiceImpl(GestoPagoProductClient gestoPagoProductClient,
                                        GestoPagoTokenService gestoPagoTokenService) {
        this.gestoPagoProductClient = gestoPagoProductClient;
        this.gestoPagoTokenService = gestoPagoTokenService;
    }

    @Override
    public ConsultaProductosResponse obtenerProductos(ConsultaProductosRequest request) {
        log.info("Iniciando invocación para obtener la lista de productos de GestoPago...");

        // 1. Obtener y construir el Bearer Token de manera dinámica (sin hardcodeo en código fuente)
        String rawToken = resolverBearerToken();
        String authorizationHeader = formatearBearerHeader(rawToken);

        // Registro de log seguro (enmascarando información sensible)
        log.info("Consumiendo endpoint getProductList.do con token enmascarado: {}", enmascararToken(rawToken));

        try {
            // 2. Realizar petición al cliente de integración OpenFeign
            GestoPagoProductXmlResponse xmlResponse = gestoPagoProductClient.getProductList(authorizationHeader);

            // 3. Validar si la respuesta del proveedor fue nula o fallida
            if (xmlResponse == null) {
                log.warn("El servicio externo GestoPago devolvió una respuesta nula.");
                return ConsultaProductosResponse.builder()
                        .codigo("99")
                        .mensaje("No se obtuvo respuesta del proveedor externo de productos.")
                        .productos(new ArrayList<>())
                        .build();
            }

            // 4. Mapear productos del XML a DTOs limpios de negocio (Garantizando Cero Nulos)
            List<ProductoDto> listaProductos = mapearProductos(xmlResponse);

            // Determinar código y mensaje de negocio proveniente del XML si existe
            String codigoNegocio = "01";
            String mensajeNegocio = "Operación realizada con éxito";
            if (xmlResponse.getMensaje() != null) {
                if (xmlResponse.getMensaje().getCodigo() != null) {
                    codigoNegocio = xmlResponse.getMensaje().getCodigo();
                }
                if (xmlResponse.getMensaje().getTexto() != null) {
                    mensajeNegocio = xmlResponse.getMensaje().getTexto();
                }
            }

            log.info("Invocación a GestoPago finalizada con éxito. Se procesaron {} productos.", listaProductos.size());

            return ConsultaProductosResponse.builder()
                    .codigo(codigoNegocio)
                    .mensaje(mensajeNegocio)
                    .productos(listaProductos)
                    .build();

        } catch (FeignException.Unauthorized e) {
            log.error("Error de autenticación 401/403 al consumir GestoPago: {}", e.getMessage());
            return ConsultaProductosResponse.builder()
                    .codigo("401")
                    .mensaje("Error de autenticación con el servicio de productos.")
                    .productos(new ArrayList<>())
                    .build();

        } catch (FeignException e) {
            log.error("Error de comunicación/HTTP con el servicio GestoPago: Estado={}, Mensaje={}", e.status(), e.getMessage());
            return ConsultaProductosResponse.builder()
                    .codigo("503")
                    .mensaje("Error de comunicación o tiempo de espera agotado con el proveedor externo.")
                    .productos(new ArrayList<>())
                    .build();

        } catch (Exception e) {
            log.error("Error inesperado en el servicio de productos: {}", e.getMessage(), e);
            return ConsultaProductosResponse.builder()
                    .codigo("500")
                    .mensaje("Ocurrió un error interno al procesar la lista de productos.")
                    .productos(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Resuelve el token de autenticación priorizando la entidad guardada en BD mediante GestoPagoTokenService,
     * utilizando la propiedad configurada en application.properties como mecanismo de respaldo (fallback).
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

    //Formatea el encabezado de autorización asegurando el prefijo "Bearer ".
    private String formatearBearerHeader(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (token.startsWith("Bearer ")) {
            return token;
        }
        return "Bearer " + token;
    }


    //Enmascara el token de autenticación para evitar la exposición de credenciales sensibles en logs de monitoreo.
    private String enmascararToken(String token) {
        if (token == null || token.length() < 10) {
            return "****";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    //Mapea la lista de productos obtenida del XML a objetos DTO limpios.
    //Garantiza que ningún atributo individual sea nulo.

    private List<ProductoDto> mapearProductos(GestoPagoProductXmlResponse xmlResponse) {
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
}
