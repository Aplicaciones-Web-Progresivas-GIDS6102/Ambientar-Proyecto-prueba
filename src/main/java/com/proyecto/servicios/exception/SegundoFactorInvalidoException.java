package com.proyecto.servicios.exception;

/**
 * Excepción cuando el código de segunda autenticación es inválido, expirado o ya fue utilizado.
 */
public class SegundoFactorInvalidoException extends RuntimeException {
    public SegundoFactorInvalidoException(String message) {
        super(message);
    }
}

