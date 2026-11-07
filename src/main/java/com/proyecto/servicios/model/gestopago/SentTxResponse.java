package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta del proveedor SentTx al enviar una transacción ({@code sendTx}).
 * Los nombres de campo coinciden con el contrato del proveedor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SentTxResponse {

    /** Código numérico de resultado. "0" = éxito, "82" = timeout/pendiente. */
    @JsonProperty("codigo")
    private String codigo;

    /** Mensaje descriptivo del proveedor. */
    @JsonProperty("mensaje")
    private String mensaje;

    /** UPC asignado por el proveedor a la transacción (correlación con confirmTx). */
    @JsonProperty("upc")
    private String upc;

    /** Datos adicionales que el proveedor pueda devolver (cifrados o en claro). */
    @JsonProperty("data")
    private String data;
}
