package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando la respuesta XML del proveedor GestoPago es inválida o no puede deserializarse.
 */
public class XmlParsingException extends RuntimeException {
    public XmlParsingException(String mensaje) {
        super(mensaje);
    }

    public XmlParsingException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
