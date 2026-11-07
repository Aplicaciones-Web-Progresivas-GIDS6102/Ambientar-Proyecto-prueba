package com.proyecto.servicios.enums;

/**
 * Estado de negocio de un pago de servicio.
 * PENDIENTE_CONFIRMACION: resultado desconocido (p. ej. timeout); requiere confirmTx, nunca reenvío ciego.
 */
public enum EstadoPagoServicio {
    APLICADO,
    RECHAZADO,
    PENDIENTE_CONFIRMACION
}
