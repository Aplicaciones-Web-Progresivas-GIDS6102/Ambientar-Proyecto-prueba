package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.ErrorResponse;
import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.core.JsonParseException;
import jakarta.validation.ConstraintViolationException;

/**
 * Manejador global de excepciones desacoplado y estandarizado con respuestas HTTP precisas.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        log.warn("Cliente no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "404", ex.getMessage());
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        log.warn("Cuenta no encontrada: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "404", ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        log.warn("Usuario no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "404", ex.getMessage());
    }

    @ExceptionHandler({ClienteYaExisteException.class, CurpDuplicadaException.class, RfcDuplicadoException.class, UsuarioYaExisteException.class})
    public ResponseEntity<ErrorResponse> handleConflictosExistencia(RuntimeException ex) {
        log.warn("Conflicto de duplicidad en entidad: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "409", ex.getMessage());
    }

    @ExceptionHandler({CredencialesInvalidasException.class, BadCredentialsException.class, SesionExpiradaException.class})
    public ResponseEntity<ErrorResponse> handleAutenticacion(RuntimeException ex) {
        log.warn("Falla de autenticación/sesión: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "401", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "403", "No cuenta con los permisos necesarios para acceder a este recurso.");
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<ErrorResponse> handleValidacionNegocio(ValidacionNegocioException ex) {
        log.warn("Falla de regla de negocio: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "400", ex.getMessage());
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

        // Estándar REST para el resto de la aplicación (HTTP 400 Bad Request)
        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo("400")
                .mensaje("Error de validación en los parámetros de entrada.")
                .estado(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .detalles(errores)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Solicitud JSON mal formada o no legible: {}", ex.getMessage());
        List<String> detalles = new ArrayList<>();

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse("solicitud");
            detalles.add(fieldName + ": El valor '" + ife.getValue() + "' no tiene un formato o tipo de dato válido.");
        } else if (cause instanceof MismatchedInputException mie) {
            String fieldName = mie.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse("solicitud");
            detalles.add(fieldName + ": El tipo de dato proporcionado es inválido.");
        } else if (cause instanceof JsonParseException) {
            detalles.add("Sintaxis JSON inválida o mal formada. Verifique las comas, comillas y llaves del JSON.");
        } else {
            detalles.add("El cuerpo de la solicitud JSON es inválido o está mal formado. Verifique la sintaxis JSON.");
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo("400")
                .mensaje("El cuerpo de la solicitud JSON es inválido o contiene tipos de datos incorrectos.")
                .estado(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .detalles(detalles)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo("400")
                .mensaje("Error de validación en los parámetros de la solicitud.")
                .estado(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .detalles(detalles)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("Violación de integridad de datos en BD: {}", ex.getMessage());
        String mensaje = "Error de integridad de datos en la base de datos.";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            mensaje += " Detalles: " + ex.getCause().getMessage();
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "400", mensaje);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Excepción no controlada en el sistema: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "500", "Ocurrió un error inesperado al procesar la solicitud.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String codigo, String mensaje) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .codigo(codigo)
                .mensaje(mensaje)
                .estado(status.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }
}
