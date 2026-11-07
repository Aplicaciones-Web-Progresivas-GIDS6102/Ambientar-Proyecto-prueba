package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.PagoServicioResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioValidado;

/**
 * Puerto hacia los servicios de transacciones y confirmación de GestoPago (sendTx.do / confirmTx.do).
 */
public interface SentTxGateway {

    /**
     * Cifra y envía la transacción al endpoint sendTx.do.
     *
     * @param pago datos validados del pago.
     * @return resultado interpretado de la operación.
     */
    PagoServicioResponse enviar(PagoServicioValidado pago);

    /**
     * Consulta el estado de una operación en timeout/pendiente mediante confirmTx.do.
     *
     * @param upc identificador único de la transacción.
     * @return estado confirmado de la operación (APLICADO, RECHAZADO o PENDIENTE_CONFIRMACION).
     */
    PagoServicioResponse confirmar(String upc);
}
