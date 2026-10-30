package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.PagoServicioResponse;

import java.math.BigDecimal;

/**
 * Caso de uso: pago de servicios GestoPago con segunda autenticación (2FA) y conciliación.
 */
public interface PagoServicioService {

    /**
     * Orquesta la validación completa del cliente, cuenta, saldo, producto, 2FA,
     * envío al proveedor y descuento de saldo condicionado al resultado exitoso.
     */
    PagoServicioResponse pagarServicio(PagoServicioRequest request, String usernameAutenticado);

    /**
     * Concilia una operación en timeout o estado pendiente consultando confirmTx.do.
     * Si el proveedor confirma la operación como APLICADA, ejecuta el descuento de saldo correspondiente.
     */
    PagoServicioResponse conciliarTransaccion(String upc);

    /**
     * Aplica el débito al saldo de la cuenta tras confirmación de pago exitoso.
     */
    void descontarSaldo(Cuenta cuenta, BigDecimal monto);
}
