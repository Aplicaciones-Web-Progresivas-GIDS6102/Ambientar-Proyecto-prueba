package com.proyecto.servicios.exception;

/**
 * Excepción para indicar duplicación del valor de RFC.
 */
public class RfcDuplicadoException extends ClienteYaExisteException {
    public RfcDuplicadoException(String message) {
        super(message);
    }
}
