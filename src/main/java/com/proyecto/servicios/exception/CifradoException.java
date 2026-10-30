package com.proyecto.servicios.exception;

/**
 * Excepción lanzada ante errores en el proceso de cifrado o descifrado AES.
 */
public class CifradoException extends RuntimeException {
    public CifradoException(String mensaje) {
        super(mensaje);
    }

    public CifradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
