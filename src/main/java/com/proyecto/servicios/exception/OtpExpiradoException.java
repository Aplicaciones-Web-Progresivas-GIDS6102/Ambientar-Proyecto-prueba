package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando el código de segunda autenticación (OTP) ha expirado.
 */
public class OtpExpiradoException extends SegundoFactorInvalidoException {
    public OtpExpiradoException(String mensaje) {
        super(mensaje);
    }
}
