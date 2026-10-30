package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * Mapeo de la respuesta XML devuelta por GestoPago en sendTx.do y confirmTx.do.
 * Soporta formato anidado:
 * <RESPONSE>
 *     <NUM_AUTORIZACION>-1</NUM_AUTORIZACION>
 *     <MENSAJE>
 *         <CODIGO>82</CODIGO>
 *         <TEXTO>Tiempo de espera alcanzado...</TEXTO>
 *     </MENSAJE>
 * </RESPONSE>
 * Y formato texto plano en MENSAJE:
 * <RESPONSE>
 *     <NUM_AUTORIZACION>-1</NUM_AUTORIZACION>
 *     <MENSAJE>Por el momento el carrier no responde...</MENSAJE>
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
        if (mensaje != null && mensaje.getTexto() != null && !mensaje.getTexto().isBlank()) {
            return mensaje.getTexto();
        }
        if (estado != null && !estado.isBlank()) {
            return estado;
        }
        return "";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(using = MensajeXmlDeserializer.class)
    public static class MensajeXml {
        @JacksonXmlProperty(localName = "CODIGO")
        private String codigo;

        @JacksonXmlProperty(localName = "TEXTO")
        private String texto;

        public MensajeXml(String textoPlano) {
            this.texto = textoPlano;
        }
    }

    public static class MensajeXmlDeserializer extends JsonDeserializer<MensajeXml> {
        @Override
        public MensajeXml deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isTextual() || node.isValueNode()) {
                return new MensajeXml(null, node.asText());
            }
            String codigo = node.has("CODIGO") ? node.get("CODIGO").asText() : null;
            String texto = node.has("TEXTO") ? node.get("TEXTO").asText() : null;
            if (texto == null && node.has("") && node.get("").isTextual()) {
                texto = node.get("").asText();
            }
            return new MensajeXml(codigo, texto != null ? texto : node.asText());
        }
    }
}
