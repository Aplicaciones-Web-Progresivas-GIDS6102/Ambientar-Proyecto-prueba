package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.SentTxRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Cliente Feign para los endpoints de transacciones y confirmación de GestoPago:
 * - POST /sistema/service/sendTx.do
 * - POST /sistema/service/confirmTx.do
 * La URL base se configura en {@code gestopago.senttx.url}.
 */
@FeignClient(
        name = "gestoPagoSentTxClient",
        url = "${gestopago.senttx.url}",
        configuration = SentTxFeignConfig.class
)
public interface GestoPagoSentTxClient {

    /**
     * Envía una transacción de pago cifrada al proveedor en el endpoint sendTx.do.
     *
     * @param authorization encabezado Bearer con el token GestoPago (Bearer <token>).
     * @param apiKey        encabezado X-API-Key del distribuidor (opcional).
     * @param request       cuerpo con el payload cifrado en el campo {@code data}.
     * @return respuesta del proveedor (cuerpo XML o JSON).
     */
    @PostMapping(
            value = "/sistema/service/sendTx.do",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.ALL_VALUE}
    )
    String sendTx(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody SentTxRequest request
    );

    /**
     * Confirma el estado de una transacción pendiente en el endpoint confirmTx.do.
     *
     * @param authorization encabezado Bearer con el token GestoPago (Bearer <token>).
     * @param apiKey        encabezado X-API-Key del distribuidor (opcional).
     * @param request       cuerpo con el UPC cifrado en el campo {@code data}.
     * @return respuesta del proveedor con el estado actualizado (XML o JSON).
     */
    @PostMapping(
            value = "/sistema/service/confirmTx.do",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.ALL_VALUE}
    )
    String confirmTx(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody SentTxRequest request
    );
}
