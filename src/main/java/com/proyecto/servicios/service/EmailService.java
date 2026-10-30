package com.proyecto.servicios.service;

import java.math.BigDecimal;

/**
 * Servicio de envío de notificaciones y códigos de seguridad por correo electrónico.
 */
public interface EmailService {

    /**
     * Envía el código OTP para autorización de pago de servicio al correo del cliente.
     *
     * @param destinatario    correo electrónico del cliente.
     * @param nombreCliente   nombre o titular de la cuenta.
     * @param codigoOtp       código OTP de 6 dígitos generado.
     * @param idServicio      identificador del servicio.
     * @param idProducto      identificador del producto.
     * @param monto           monto del pago.
     * @param vigenciaMinutos tiempo de vigencia en minutos.
     */
    void enviarOtpPago(String destinatario, String nombreCliente, String codigoOtp,
                       String idServicio, String idProducto, BigDecimal monto, int vigenciaMinutos);
}
