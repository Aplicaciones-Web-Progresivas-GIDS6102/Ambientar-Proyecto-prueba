package com.proyecto.servicios.exception;

public class SesionExpiradaException extends RuntimeException {
    public SesionExpiradaException(String message) {
        super(message);
    }
}
