package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.Solicitud2FARequestDTO;
import com.proyecto.servicios.handler.PagoServicioHandler;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para el flujo completo de pagos de servicios GestoPago.
 * Requiere autenticación JWT.
 * Sigue la arquitectura por capas delegando toda la orquestación a {@link PagoServicioHandler}.
 */
@RestController
@RequestMapping("/api/v1/gestopago/pagos")
@RequiredArgsConstructor
@Tag(name = "GestoPago Pagos", description = "Endpoints para segunda autenticación (2FA), procesamiento de pagos y conciliación")
public class GestoPagoPagoController {

    private final PagoServicioHandler pagoServicioHandler;

    @Operation(
            summary = "Solicitar segunda autenticación (2FA)",
            description = "Valida identidad, cuenta, saldo y producto, genera un código OTP seguro y lo envía al correo registrado vía Brevo."
    )
    @PostMapping(
            value = {"/solicitar-2fa", "/otp"},
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenericResponse> solicitar2FA(@Valid @RequestBody Solicitud2FARequestDTO request,
                                                        Authentication authentication) {
        validarAutenticacion(authentication);
        GenericResponse response = pagoServicioHandler.solicitar2FA(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Pagar servicio",
            description = "Valida cuenta, saldo, producto y código 2FA. Cifra el payload en AES y lo envía a GestoPago. Devuelve código interno 0 en caso de éxito."
    )
    @PostMapping(
            value = {"", "/", "/servicios"},
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenericResponse> pagarServicio(@Valid @RequestBody PagoServicioRequest request,
                                                         Authentication authentication) {
        validarAutenticacion(authentication);
        return pagoServicioHandler.procesarPago(request, authentication.getName());
    }

    @Operation(
            summary = "Conciliar transacción pendiente",
            description = "Consulta confirmTx.do para determinar el estado de transacciones en timeout (Applied / Not Applied) y aplica el débito si procede."
    )
    @PostMapping(
            value = "/confirmar/{upc}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GenericResponse> conciliarTransaccion(@PathVariable("upc") String upc,
                                                                Authentication authentication) {
        validarAutenticacion(authentication);
        GenericResponse response = pagoServicioHandler.conciliarTransaccion(upc);
        return ResponseEntity.ok(response);
    }

    private void validarAutenticacion(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Autenticación requerida para realizar operaciones de pago.");
        }
    }
}
