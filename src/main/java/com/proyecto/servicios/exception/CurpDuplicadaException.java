package com.proyecto.servicios.exception;

/**
 * Excepción para indicar duplicación del valor de CURP.
 */
public class CurpDuplicadaException extends ClienteYaExisteException {
    public CurpDuplicadaException(String message) {
        super(message);
    }
}
