package com.proyecto.servicios.exception;

/**
 * Excepción de negocio lanzada cuando se intenta realizar una operación
 * incompatible con un cliente que ya se encuentra dado de baja lógica.
 */
public class ClienteYaEliminadoException extends RuntimeException {

    public ClienteYaEliminadoException(String message) {
        super(message);
    }
}
