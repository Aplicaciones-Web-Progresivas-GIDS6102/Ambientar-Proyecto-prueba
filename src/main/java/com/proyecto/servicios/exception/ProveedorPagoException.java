package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando el proveedor GestoPago rechaza la operación de pago.
 */
public class ProveedorPagoException extends RuntimeException {
    public ProveedorPagoException(String mensaje) {
        super(mensaje);
    }
}
