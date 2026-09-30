package com.proyecto.servicios.exception;

/**
 * Excepción para indicar violación de reglas de negocio en operaciones del sistema.
 */
public class ValidacionNegocioException extends RuntimeException {
    public ValidacionNegocioException(String message) {
        super(message);
    }
}
