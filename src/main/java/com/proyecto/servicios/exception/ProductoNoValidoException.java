package com.proyecto.servicios.exception;

/**
 * Excepción cuando el servicio/producto GestoPago no existe, no corresponde o el monto no es permitido.
 */
public class ProductoNoValidoException extends RuntimeException {
    public ProductoNoValidoException(String message) {
        super(message);
    }
}

