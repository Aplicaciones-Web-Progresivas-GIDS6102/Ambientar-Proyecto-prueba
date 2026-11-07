package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando se detecta una operación de pago duplicada o previamente procesada.
 */
public class OperacionDuplicadaException extends RuntimeException {
    public OperacionDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
