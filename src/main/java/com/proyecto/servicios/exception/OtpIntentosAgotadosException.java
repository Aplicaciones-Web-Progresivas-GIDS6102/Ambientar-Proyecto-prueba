package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando se superan los 3 intentos máximos permitidos para validar el OTP.
 */
public class OtpIntentosAgotadosException extends SegundoFactorInvalidoException {
    public OtpIntentosAgotadosException(String mensaje) {
        super(mensaje);
    }
}
