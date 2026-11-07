package com.proyecto.servicios.exception;

/**
 * Excepción para indicar que un cliente no existe o no fue encontrado en la base de datos.
 */
public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException(String message) {
        super(message);
    }
}
