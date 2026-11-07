package com.proyecto.servicios.exception;

/**
 * Excepción para indicar que una cuenta bancaria no existe o no fue encontrada.
 */
public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(String message) {
        super(message);
    }
}
