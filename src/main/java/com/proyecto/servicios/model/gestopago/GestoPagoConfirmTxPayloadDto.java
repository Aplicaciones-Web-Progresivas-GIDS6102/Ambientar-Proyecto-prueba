package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload JSON cifrado para confirmTx.do.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoConfirmTxPayloadDto {

    @JsonProperty("metodo")
    @Builder.Default
    private String metodo = "confirmTx";

    @JsonProperty("upc")
    private String upc;
}
