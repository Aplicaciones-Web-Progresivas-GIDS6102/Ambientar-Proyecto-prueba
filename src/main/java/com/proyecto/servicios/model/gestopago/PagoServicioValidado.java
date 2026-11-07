package com.proyecto.servicios.model.gestopago;

import java.math.BigDecimal;

/**
 * Datos de un pago que ya superó todas las validaciones internas (cuenta, saldo, producto, 2FA).
 * Es la única entrada que recibe el gateway del proveedor; no incluye el código 2FA.
 */
public record PagoServicioValidado(
        Long cuentaId,
        Long clienteId,
        String idServicio,
        String idProducto,
        BigDecimal monto,
        String moneda,
        String telefono,
        String referencia
) {
}
