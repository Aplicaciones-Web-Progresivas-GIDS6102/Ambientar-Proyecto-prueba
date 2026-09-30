package com.proyecto.servicios.config;

import com.proyecto.servicios.model.ErrorResponse;
import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manejador global de excepciones desacoplado.
 * 
 * Justificación Técnica: Permite que el código legacy de GestoPago conserve su contrato
 * específico (HTTP 200 con ConsultaProductosResponse), mientras que los demás endpoints
 * utilicen el estándar REST (ErrorResponse con códigos de estado HTTP semánticos 400, 500, etc.).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errores = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            String msg = fieldError.getDefaultMessage();
            sb.append(msg);
            errores.add(fieldError.getField() + ": " + msg);
        }

        String mensajeError = sb.length() > 0 ? sb.toString() : "Error en los parámetros de entrada de la petición.";
        log.warn("Falla de validación en Request: {}", mensajeError);

        // Compatibilidad Legacy para GestoPago (mantiene HTTP 200 OK con DTO ConsultaProductosResponse)
        if (ex.getBindingResult().getTarget() instanceof ConsultaProductosRequest) {
            ConsultaProductosResponse legacyResponse = ConsultaProductosResponse.builder()
                    .codigo("400")
                    .mensaje(mensajeError)
                    .productos(new ArrayList<>())
                    .build();
            return new ResponseEntity<>(legacyResponse, HttpStatus.OK);
        }

        // Estándar REST para el resto de la aplicación (HTTP 400 Bad Request con ErrorResponse)
        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo("400")
                .mensaje("Error de validación en los parámetros de entrada.")
                .estado(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .detalles(errores)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Excepción no controlada en el sistema: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo("500")
                .mensaje("Ocurrió un error inesperado al procesar la solicitud.")
                .estado(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
