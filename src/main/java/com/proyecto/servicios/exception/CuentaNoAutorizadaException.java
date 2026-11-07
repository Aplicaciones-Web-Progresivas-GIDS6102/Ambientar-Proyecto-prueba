package com.proyecto.servicios.exception;

/**
 * Excepción cuando la cuenta no pertenece al usuario autenticado o no puede operar.
 */
public class CuentaNoAutorizadaException extends RuntimeException {
    public CuentaNoAutorizadaException(String message) {
        super(message);
    }
}

