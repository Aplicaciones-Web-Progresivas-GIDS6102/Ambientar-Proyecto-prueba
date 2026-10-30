package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mapeo de la respuesta XML devuelta por GestoPago en sendTx.do y confirmTx.do.
 * Ejemplo:
 * <RESPONSE>
 *     <NUM_AUTORIZACION>-1</NUM_AUTORIZACION>
 *     <MENSAJE>
 *         <CODIGO>82</CODIGO>
 *         <TEXTO>Tiempo de espera alcanzado, la transacción no ha sido registrada</TEXTO>
 *     </MENSAJE>
 * </RESPONSE>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPONSE")
public class GestoPagoTxXmlResponse {

    @JacksonXmlProperty(localName = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @JacksonXmlProperty(localName = "MENSAJE")
    private MensajeXml mensaje;

    @JacksonXmlProperty(localName = "UPC")
    private String upc;

    @JacksonXmlProperty(localName = "ESTADO")
    private String estado;

    /**
     * Helper para obtener el código funcional independientemente de si viene dentro de MENSAJE o directo.
     */
    public String obtenerCodigo() {
        if (mensaje != null && mensaje.getCodigo() != null) {
            return mensaje.getCodigo();
        }
        return null;
    }

    /**
     * Helper para obtener el texto del mensaje.
     */
    public String obtenerTexto() {
        if (mensaje != null && mensaje.getTexto() != null) {
            return mensaje.getTexto();
        }
        if (estado != null) {
            return estado;
        }
        return "";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MensajeXml {
        @JacksonXmlProperty(localName = "CODIGO")
        private String codigo;

        @JacksonXmlProperty(localName = "TEXTO")
        private String texto;
    }
}
