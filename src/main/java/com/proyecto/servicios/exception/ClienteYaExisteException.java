package com.proyecto.servicios.exception;

/**
 * Excepción base para indicar un conflicto de existencia o duplicación de cliente.
 */
public class ClienteYaExisteException extends RuntimeException {
    public ClienteYaExisteException(String message) {
        super(message);
    }
}
