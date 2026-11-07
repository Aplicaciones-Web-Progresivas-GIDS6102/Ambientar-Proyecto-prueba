package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload JSON que se construye y se cifra con AES antes de enviarse a GestoPago sendTx.do.
 * Contempla:
 * - Método: sendTx
 * - ID del producto
 * - ID del servicio
 * - Teléfono o referencia
 * - Hora local
 * - Monto
 * - Unidad
 * - UPC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoSendTxPayloadDto {

    @JsonProperty("metodo")
    @Builder.Default
    private String metodo = "sendTx";

    @JsonProperty("idProducto")
    private String idProducto;

    @JsonProperty("idServicio")
    private String idServicio;

    @JsonProperty("telefono")
    private String telefono;

    @JsonProperty("referencia")
    private String referencia;

    @JsonProperty("hora")
    private String hora;

    @JsonProperty("monto")
    private String monto;

    @JsonProperty("unidad")
    @Builder.Default
    private String unidad = "1";

    @JsonProperty("upc")
    private String upc;
}
