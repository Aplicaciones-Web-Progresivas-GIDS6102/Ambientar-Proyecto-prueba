package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuerpo de la petición POST al endpoint sendTx del proveedor SentTx (GestoPago).
 * El campo {@code data} transporta el payload cifrado con AES/CBC/PKCS5Padding en Base64.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SentTxRequest {

    /** Payload cifrado en Base64 (AES/CBC/PKCS5Padding). */
    private String data;
}
