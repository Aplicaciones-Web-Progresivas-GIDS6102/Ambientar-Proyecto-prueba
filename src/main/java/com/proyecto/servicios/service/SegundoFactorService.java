package com.proyecto.servicios.service;

import java.math.BigDecimal;

/**
 * Puerto de segunda autenticación (2FA) interna del cliente exclusiva para operaciones de pago.
 * Garantiza:
 * - Código seguro y aleatorio.
 * - Asociación unívoca con el cliente y la operación de pago.
 * - Envío seguro mediante Brevo.
 * - Vigencia limitada (5 minutos).
 * - Máximo de 3 intentos de validación.
 * - Invalidación inmediata tras uso (uso único) o agotamiento de intentos.
 * - Almacenamiento seguro mediante hashing BCrypt (nunca en texto plano).
 */
public interface SegundoFactorService {

    /**
     * Genera un código OTP de 6 dígitos, lo persiste cifrado/hasheado asociado a la operación
     * y lo envía al correo del cliente mediante Brevo.
     *
     * @param clienteId     identificador del cliente.
     * @param correo        correo electrónico del cliente.
     * @param nombreCliente nombre del cliente para el correo.
     * @param cuentaId      identificador de la cuenta bancaria origen.
     * @param idServicio    identificador del servicio a pagar.
     * @param idProducto    identificador del producto a pagar.
     * @param monto         monto monetario de la operación.
     * @return tiempo de vigencia en segundos (ej. 300).
     */
    int solicitarOtp(Long clienteId, String correo, String nombreCliente, Long cuentaId,
                     String idServicio, String idProducto, BigDecimal monto);

    /**
     * Valida y consume el código OTP para una operación de pago específica.
     *
     * @param clienteId  identificador del cliente autenticado.
     * @param cuentaId   identificador de la cuenta origen.
     * @param idServicio identificador del servicio a pagar.
     * @param idProducto identificador del producto a pagar.
     * @param monto      monto monetario de la operación.
     * @param codigo     código OTP ingresado por el usuario.
     * @return true si el código es válido y queda consumido (uso único).
     */
    boolean validarYConsumir(Long clienteId, Long cuentaId, String idServicio, String idProducto, BigDecimal monto, String codigo);

    /**
     * Sobrecarga retrocompatible: valida y consume el último OTP activo del cliente.
     *
     * @param clienteId identificador del cliente autenticado.
     * @param codigo    código OTP ingresado por el usuario.
     * @return true si el código es válido y queda consumido (uso único).
     */
    boolean validarYConsumir(Long clienteId, String codigo);
}
