package com.proyecto.servicios.exception;

/**
 * Excepción cuando la cuenta no tiene saldo disponible suficiente para la operación.
 */
public class SaldoInsuficienteException extends RuntimeException {
    public SaldoInsuficienteException(String message) {
        super(message);
    }
}

