package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.proyecto.servicios.client.GestoPagoSentTxClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import com.proyecto.servicios.exception.IntegracionNoDisponibleException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.exception.XmlParsingException;
import com.proyecto.servicios.model.gestopago.*;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.service.AesCipherService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.SentTxGateway;
import com.proyecto.servicios.service.UpcGenerator;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Implementación concreta del gateway SentTx (GestoPago) para sendTx.do y confirmTx.do.
 * <p>
 * Flujo de ejecución:
 * <ol>
 *   <li>Genera un UPC único mediante {@link UpcGenerator}.</li>
 *   <li>Construye el DTO específico de GestoPago (sendTx) con método, montos y hora.</li>
 *   <li>Cifra el payload en AES/CBC/PKCS5Padding con clave e IV configurados.</li>
 *   <li>Obtiene y administra el token Bearer (vigencia de 24 horas, renovado según aplique).</li>
 *   <li>Envía la petición HTTP POST a sendTx.do.</li>
 *   <li>Interpreta la respuesta XML del proveedor: código '01' éxito, '82' timeout.</li>
 *   <li>Ante timeout (>62s) o respuesta 82, establece PENDIENTE_CONFIRMACION sin duplicar UPC ni reenviar cobros.</li>
 *   <li>Soporta confirmación y conciliación mediante confirmTx.do ('Applied' / 'Not Applied').</li>
 * </ol>
 */
@Service
@Slf4j
public class SentTxGatewayImpl implements SentTxGateway {

    private static final String CODIGO_PROVEEDOR_EXITO = "01";
    private static final String CODIGO_PROVEEDOR_EXITO_ALT = "0";
    private static final String CODIGO_TIMEOUT = "82";
    private static final DateTimeFormatter HORA_FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GestoPagoSentTxClient sentTxClient;
    private final AesCipherService aesCipherService;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoTransaccionRepository transaccionRepository;
    private final UpcGenerator upcGenerator;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.senttx.api-key:}")
    private String apiKey;

    @Value("${gestopago.simulacion.exitosa:false}")
    private boolean simulacionExitosa;

    public SentTxGatewayImpl(GestoPagoSentTxClient sentTxClient,
                             AesCipherService aesCipherService,
                             GestoPagoTokenService tokenService,
                             GestoPagoTransaccionRepository transaccionRepository,
                             UpcGenerator upcGenerator,
                             ObjectMapper objectMapper) {
        this.sentTxClient = sentTxClient;
        this.aesCipherService = aesCipherService;
        this.tokenService = tokenService;
        this.transaccionRepository = transaccionRepository;
        this.upcGenerator = upcGenerator;
        this.objectMapper = objectMapper;
        this.xmlMapper = new XmlMapper();
    }

    @Override
    public PagoServicioResponse enviar(PagoServicioValidado pago) {
        String upc = upcGenerator.generarUpc();
        String payloadJson = construirPayloadSendTxJson(pago, upc);
        String payloadCifrado = aesCipherService.cifrar(payloadJson);
        String bearerToken = resolverBearerToken();

        log.info("Enviando sendTx.do al proveedor. upc={}, idServicio={}, idProducto={}",
                upc, pago.idServicio(), pago.idProducto());

        // 1. Persistir transacción en BD como PENDIENTE antes de llamar al proveedor
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .cuentaId(pago.cuentaId())
                .clienteId(pago.clienteId())
                .idServicio(pago.idServicio())
                .idProducto(pago.idProducto())
                .monto(pago.monto())
                .moneda(pago.moneda() != null ? pago.moneda() : "MXN")
                .telefono(pago.telefono())
                .referencia(pago.referencia())
                .upc(upc)
                .estado("PENDIENTE")
                .build();
        transaccionRepository.save(tx);

        if (simulacionExitosa) {
            log.info("ℹ️ [MODO SIMULACIÓN ACTIVO] Simulando respuesta exitosa del proveedor GestoPago para upc={}", upc);
            actualizarTransaccion(tx, "APLICADO", CODIGO_PROVEEDOR_EXITO, "Transacción aplicada exitosamente (Modo Simulación).");
            return PagoServicioResponse.builder()
                    .estado(EstadoPagoServicio.APLICADO)
                    .upc(upc)
                    .idServicio(pago.idServicio())
                    .idProducto(pago.idProducto())
                    .monto(pago.monto())
                    .mensaje("Pago aplicado exitosamente.")
                    .build();
        }

        String respuestaRaw;
        try {
            SentTxRequest request = SentTxRequest.builder().data(payloadCifrado).build();
            respuestaRaw = sentTxClient.sendTx("Bearer " + bearerToken, apiKey, request);
        } catch (FeignException.Forbidden e) {
            log.warn("HTTP 403 del proveedor. Invalidando token y forzando renovación. upc={}", upc);
            tokenService.renovarToken();
            actualizarTransaccion(tx, "PENDIENTE_CONFIRMACION", "403", "Token expirado; se forzó renovación.");
            throw new IntegracionNoDisponibleException(
                    "Token rechazado por el proveedor (403). Se renovó automáticamente. Reintente la operación.");
        } catch (FeignException e) {
            log.error("Error HTTP {} del proveedor al enviar sendTx.do. upc={}", e.status(), upc, e);
            actualizarTransaccion(tx, "PENDIENTE_CONFIRMACION", String.valueOf(e.status()),
                    "Error de comunicación con el proveedor: HTTP " + e.status());
            return PagoServicioResponse.builder()
                    .estado(EstadoPagoServicio.PENDIENTE_CONFIRMACION)
                    .upc(upc)
                    .idServicio(pago.idServicio())
                    .idProducto(pago.idProducto())
                    .monto(pago.monto())
                    .mensaje("Error de comunicación con el proveedor. Consulte el estado antes de reintentar.")
                    .build();
        } catch (Exception e) {
            log.error("Excepción inesperada al enviar sendTx.do. upc={}", upc, e);
            actualizarTransaccion(tx, "PENDIENTE_CONFIRMACION", null, "Excepción: " + e.getMessage());
            return PagoServicioResponse.builder()
                    .estado(EstadoPagoServicio.PENDIENTE_CONFIRMACION)
                    .upc(upc)
                    .idServicio(pago.idServicio())
                    .idProducto(pago.idProducto())
                    .monto(pago.monto())
                    .mensaje("Error inesperado al comunicarse con el proveedor. Consulte el estado antes de reintentar.")
                    .build();
        }

        return mapearRespuestaSendTx(respuestaRaw, pago, upc, tx);
    }

    @Override
    public PagoServicioResponse confirmar(String upc) {
        if (upc == null || upc.isBlank()) {
            throw new ValidacionNegocioException("El UPC es obligatorio para confirmar la operación.");
        }

        GestoPagoTransaccion tx = transaccionRepository.findByUpc(upc)
                .orElseThrow(() -> new ValidacionNegocioException("No se encontró la transacción con UPC: " + upc));

        if ("APLICADO".equalsIgnoreCase(tx.getEstado())) {
            return PagoServicioResponse.builder()
                    .estado(EstadoPagoServicio.APLICADO)
                    .upc(upc)
                    .idServicio(tx.getIdServicio())
                    .idProducto(tx.getIdProducto())
                    .monto(tx.getMonto())
                    .mensaje("La transacción ya se encuentra confirmada como APLICADA.")
                    .build();
        }

        log.info("Consultando confirmTx.do para transacción upc={}", upc);

        if (simulacionExitosa) {
            actualizarTransaccion(tx, "APLICADO", CODIGO_PROVEEDOR_EXITO, "Transacción confirmada exitosamente (Modo Simulación).");
            return PagoServicioResponse.builder()
                    .estado(EstadoPagoServicio.APLICADO)
                    .upc(upc)
                    .idServicio(tx.getIdServicio())
                    .idProducto(tx.getIdProducto())
                    .monto(tx.getMonto())
                    .mensaje("Resultado de conciliación para UPC " + upc + ": APLICADO")
                    .build();
        }

        String payloadJson = construirPayloadConfirmTxJson(upc);
        String payloadCifrado = aesCipherService.cifrar(payloadJson);
        String bearerToken = resolverBearerToken();

        String respuestaRaw;
        try {
            SentTxRequest request = SentTxRequest.builder().data(payloadCifrado).build();
            respuestaRaw = sentTxClient.confirmTx("Bearer " + bearerToken, apiKey, request);
        } catch (Exception e) {
            log.error("Error al consultar confirmTx.do para upc={}: {}", upc, e.getMessage());
            return PagoServicioResponse.builder()
                    .estado(EstadoPagoServicio.PENDIENTE_CONFIRMACION)
                    .upc(upc)
                    .idServicio(tx.getIdServicio())
                    .idProducto(tx.getIdProducto())
                    .monto(tx.getMonto())
                    .mensaje("No se pudo obtener el estado del proveedor. La transacción continúa pendiente.")
                    .build();
        }

        return mapearRespuestaConfirmTx(respuestaRaw, upc, tx);
    }

    // ───────────────── Métodos privados ─────────────────

    private String construirPayloadSendTxJson(PagoServicioValidado pago, String upc) {
        GestoPagoSendTxPayloadDto payload = GestoPagoSendTxPayloadDto.builder()
                .metodo("sendTx")
                .idProducto(pago.idProducto())
                .idServicio(pago.idServicio())
                .telefono(pago.telefono())
                .referencia(pago.referencia())
                .hora(LocalDateTime.now().format(HORA_FORMATO))
                .monto(pago.monto().toPlainString())
                .unidad("1")
                .upc(upc)
                .build();

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar el payload de pago sendTx.", e);
        }
    }

    private String construirPayloadConfirmTxJson(String upc) {
        GestoPagoConfirmTxPayloadDto payload = GestoPagoConfirmTxPayloadDto.builder()
                .metodo("confirmTx")
                .upc(upc)
                .build();

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar el payload de confirmTx.", e);
        }
    }

    private String resolverBearerToken() {
        Optional<GestoPagoToken> tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
        if (tokenOpt.isEmpty() || tokenOpt.get().getToken() == null || tokenOpt.get().getToken().isBlank()) {
            log.info("No se encontró token activo en base de datos. Solicitando renovación...");
            tokenService.renovarToken();
            tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
        }

        GestoPagoToken token = tokenOpt.orElseThrow(() -> new IntegracionNoDisponibleException(
                "No hay token activo del proveedor GestoPago. El pago no puede procesarse."));

        if (token.getToken() == null || token.getToken().isBlank()) {
            throw new IntegracionNoDisponibleException(
                    "El token del proveedor GestoPago está vacío. El pago no puede procesarse.");
        }
        return token.getToken();
    }

    private PagoServicioResponse mapearRespuestaSendTx(String respuestaRaw, PagoServicioValidado pago,
                                                       String upc, GestoPagoTransaccion tx) {
        if (respuestaRaw == null || respuestaRaw.isBlank()) {
            log.warn("Respuesta nula o vacía del proveedor. upc={}", upc);
            actualizarTransaccion(tx, "PENDIENTE_CONFIRMACION", null, "Respuesta no concluyente del proveedor.");
            return PagoServicioResponse.builder()
                    .estado(EstadoPagoServicio.PENDIENTE_CONFIRMACION)
                    .upc(upc)
                    .idServicio(pago.idServicio())
                    .idProducto(pago.idProducto())
                    .monto(pago.monto())
                    .mensaje("Respuesta no concluyente del proveedor. Consulte el estado antes de reintentar.")
                    .build();
        }

        GestoPagoTxXmlResponse xmlResp = parsearRespuesta(respuestaRaw);
        String codigo = xmlResp != null ? xmlResp.obtenerCodigo() : null;
        String mensaje = xmlResp != null ? xmlResp.obtenerTexto() : "Sin descripción";

        EstadoPagoServicio estado;
        if (CODIGO_PROVEEDOR_EXITO.equals(codigo) || CODIGO_PROVEEDOR_EXITO_ALT.equals(codigo)) {
            // Proveedor responde con código '01' -> Pago confirmado exitoso
            estado = EstadoPagoServicio.APLICADO;
            actualizarTransaccion(tx, "APLICADO", codigo, mensaje);
            log.info("Pago confirmado exitosamente por el proveedor. upc={}, auth={}",
                    upc, xmlResp != null ? xmlResp.getNumAutorizacion() : "");
        } else if (CODIGO_TIMEOUT.equals(codigo)) {
            // Proveedor responde con código '82' -> Timeout / no registrada
            estado = EstadoPagoServicio.PENDIENTE_CONFIRMACION;
            actualizarTransaccion(tx, "PENDIENTE_CONFIRMACION", codigo, mensaje);
            log.warn("Proveedor reporta timeout (código 82). Transacción pendiente de confirmTx. upc={}", upc);
        } else {
            // Cualquier otro código del proveedor es rechazo
            estado = EstadoPagoServicio.RECHAZADO;
            actualizarTransaccion(tx, "RECHAZADO", codigo, mensaje);
            log.warn("Pago rechazado por el proveedor. upc={}, codigo={}, mensaje={}", upc, codigo, mensaje);
        }

        String upcFinal = (xmlResp != null && xmlResp.getUpc() != null) ? xmlResp.getUpc() : upc;

        return PagoServicioResponse.builder()
                .estado(estado)
                .upc(upcFinal)
                .idServicio(pago.idServicio())
                .idProducto(pago.idProducto())
                .monto(pago.monto())
                .mensaje(mensaje)
                .build();
    }

    private PagoServicioResponse mapearRespuestaConfirmTx(String respuestaRaw, String upc, GestoPagoTransaccion tx) {
        if (respuestaRaw == null || respuestaRaw.isBlank()) {
            return PagoServicioResponse.builder()
                    .estado(EstadoPagoServicio.PENDIENTE_CONFIRMACION)
                    .upc(upc)
                    .idServicio(tx.getIdServicio())
                    .idProducto(tx.getIdProducto())
                    .monto(tx.getMonto())
                    .mensaje("Respuesta de confirmación no concluyente.")
                    .build();
        }

        GestoPagoTxXmlResponse xmlResp = parsearRespuesta(respuestaRaw);
        String estadoProveedor = (xmlResp != null && xmlResp.getEstado() != null) ? xmlResp.getEstado() : "";
        String texto = xmlResp != null ? xmlResp.obtenerTexto() : "";
        String codigo = xmlResp != null ? xmlResp.obtenerCodigo() : null;

        EstadoPagoServicio nuevoEstado;
        if ("Applied".equalsIgnoreCase(estadoProveedor) || texto.contains("Applied") || CODIGO_PROVEEDOR_EXITO.equals(codigo)) {
            nuevoEstado = EstadoPagoServicio.APLICADO;
            actualizarTransaccion(tx, "APLICADO", codigo != null ? codigo : "Applied", texto);
            log.info("Transacción confirmada como APLICADA vía confirmTx.do. upc={}", upc);
        } else if ("Not Applied".equalsIgnoreCase(estadoProveedor) || texto.contains("Not Applied")) {
            nuevoEstado = EstadoPagoServicio.RECHAZADO;
            actualizarTransaccion(tx, "RECHAZADO", codigo != null ? codigo : "Not Applied", texto);
            log.info("Transacción confirmada como RECHAZADA vía confirmTx.do. upc={}", upc);
        } else {
            nuevoEstado = EstadoPagoServicio.PENDIENTE_CONFIRMACION;
            log.warn("Estado indeterminado devuelto por confirmTx.do para upc={}", upc);
        }

        return PagoServicioResponse.builder()
                .estado(nuevoEstado)
                .upc(upc)
                .idServicio(tx.getIdServicio())
                .idProducto(tx.getIdProducto())
                .monto(tx.getMonto())
                .mensaje(texto)
                .build();
    }

    private GestoPagoTxXmlResponse parsearRespuesta(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("<")) {
            try {
                return xmlMapper.readValue(trimmed, GestoPagoTxXmlResponse.class);
            } catch (Exception e) {
                log.error("Falla al parsear XML de GestoPago: {}", e.getMessage());
                throw new XmlParsingException("Error al procesar la respuesta XML del proveedor GestoPago.", e);
            }
        } else if (trimmed.startsWith("{")) {
            try {
                SentTxResponse json = objectMapper.readValue(trimmed, SentTxResponse.class);
                return GestoPagoTxXmlResponse.builder()
                        .upc(json.getUpc())
                        .numAutorizacion(json.getData())
                        .mensaje(new GestoPagoTxXmlResponse.MensajeXml(json.getCodigo(), json.getMensaje()))
                        .estado(json.getMensaje())
                        .build();
            } catch (Exception e) {
                log.error("Falla al parsear JSON de GestoPago: {}", e.getMessage());
                throw new RuntimeException("Error al procesar la respuesta JSON del proveedor.", e);
            }
        } else {
            // Cadena de texto plano (ej. "Applied", "Not Applied")
            return GestoPagoTxXmlResponse.builder()
                    .estado(trimmed)
                    .mensaje(new GestoPagoTxXmlResponse.MensajeXml(trimmed, trimmed))
                    .build();
        }
    }

    private void actualizarTransaccion(GestoPagoTransaccion tx, String estado,
                                       String codigoRespuesta, String mensajeRespuesta) {
        tx.setEstado(estado);
        tx.setCodigoRespuesta(codigoRespuesta);
        tx.setMensajeRespuesta(mensajeRespuesta);
        transaccionRepository.save(tx);
    }
}
