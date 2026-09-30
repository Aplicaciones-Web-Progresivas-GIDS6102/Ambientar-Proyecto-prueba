package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.*;
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
 * Permite que el código legacy de GestoPago conserve su contrato específico,
 * mientras que el sistema bancario utiliza respuestas REST estandarizadas.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        log.warn("Cliente no encontrado: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo("404")
                .mensaje(ex.getMessage())
                .estado(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        log.warn("Cuenta no encontrada: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo("404")
                .mensaje(ex.getMessage())
                .estado(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ClienteYaExisteException.class)
    public ResponseEntity<ErrorResponse> handleClienteYaExiste(ClienteYaExisteException ex) {
        log.warn("Conflicto de existencia de cliente: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo("409")
                .mensaje(ex.getMessage())
                .estado(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<ErrorResponse> handleValidacionNegocio(ValidacionNegocioException ex) {
        log.warn("Falla de regla de negocio: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo("400")
                .mensaje(ex.getMessage())
                .estado(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

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
