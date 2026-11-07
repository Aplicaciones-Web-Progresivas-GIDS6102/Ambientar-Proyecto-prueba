package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

 //DTO que mapea la raíz del XML <RESPONSE> devuelto por el servicio externo GestoPago.
 //Utiliza anotaciones de Jackson XML para deserializar los nodos <MENSAJE> y <PRODUCTOS>.

@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "RESPONSE")
public class GestoPagoProductXmlResponse {

    //Nodo <MENSAJE> que contiene el código y texto de respuesta del servicio externo.

    @JacksonXmlProperty(localName = "MENSAJE")
    private MensajeXml mensaje;

    //Nodo <PRODUCTOS> que envuelve la lista de elementos <producto>.

    @JacksonXmlProperty(localName = "PRODUCTOS")
    private ProductosWrapper productos;

    //Clase interna estática para mapear el nodo <MENSAJE> del XML.

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MensajeXml {
        @JacksonXmlProperty(localName = "CODIGO")
        private String codigo;

        @JacksonXmlProperty(localName = "TEXTO")
        private String texto;
    }

    //Clase interna estática para envolver la lista de elementos <producto> en <PRODUCTOS>.

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductosWrapper {
        @JacksonXmlProperty(localName = "producto")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<ProductoXml> listaProductos = new ArrayList<>();
    }

    //Clase interna estática para mapear cada etiqueta <producto> del XML.
    //Mapea tanto los atributos XML (servicio, producto, precio, etc.) como el elemento hijo <legend>.

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoXml {
        @JacksonXmlProperty(isAttribute = true)
        private String servicio;

        @JacksonXmlProperty(isAttribute = true)
        private String producto;

        @JacksonXmlProperty(isAttribute = true)
        private String idServicio;

        @JacksonXmlProperty(isAttribute = true)
        private String idProducto;

        @JacksonXmlProperty(isAttribute = true)
        private String idCatTipoServicio;

        @JacksonXmlProperty(isAttribute = true)
        private String tipoFront;

        @JacksonXmlProperty(isAttribute = true)
        private String hasDigitoVerificador;

        @JacksonXmlProperty(isAttribute = true)
        private String precio;

        @JacksonXmlProperty(isAttribute = true)
        private String showAyuda;

        @JacksonXmlProperty(isAttribute = true)
        private String tipoReferencia;

        @JacksonXmlProperty(localName = "legend")
        private String legend;
    }
}
