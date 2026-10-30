package com.proyecto.servicios.handler;

import com.proyecto.servicios.dto.ConfirmacionTxResponseDTO;
import com.proyecto.servicios.dto.Solicitud2FARequestDTO;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import org.springframework.http.ResponseEntity;

/**
 * Handler para orquestar el flujo de pagos de servicios GestoPago:
 * - Coordinar solicitud de 2FA vía Brevo.
 * - Coordinar validaciones y ejecución de pago.
 * - Coordinar conciliación de transacciones pendientes vía confirmTx.
 */
public interface PagoServicioHandler {

    /**
     * Orquesta la solicitud y envío del código OTP de segunda autenticación.
     */
    GenericResponse solicitar2FA(Solicitud2FARequestDTO request, String usernameAutenticado);

    /**
     * Orquesta la ejecución del pago, retorno de código interno '0' en éxito y estados HTTP correspondientes.
     */
    ResponseEntity<GenericResponse> procesarPago(PagoServicioRequest request, String usernameAutenticado);

    /**
     * Orquesta la conciliación de una transacción pendiente.
     */
    GenericResponse conciliarTransaccion(String upc);
}
