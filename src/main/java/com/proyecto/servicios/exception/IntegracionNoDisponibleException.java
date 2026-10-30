package com.proyecto.servicios.exception;

/**
 * Excepción cuando un componente requerido (2FA, proveedor) no está disponible o configurado.
 */
public class IntegracionNoDisponibleException extends RuntimeException {
    public IntegracionNoDisponibleException(String message) {
        super(message);
    }
}

