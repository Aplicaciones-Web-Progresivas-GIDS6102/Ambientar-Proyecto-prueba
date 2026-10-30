package com.proyecto.servicios.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.GenericResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones centralizado con respuestas semánticas y uniformes
 * basadas en el estándar GenericResponse y el patrón ValidadorCampos.
 * Evita la exposición de trazas de error internas o detalles de base de datos a clientes externos.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<GenericResponse> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        log.warn("Cliente no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, 404, ex.getMessage());
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<GenericResponse> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        log.warn("Cuenta no encontrada: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, 404, ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<GenericResponse> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        log.warn("Usuario no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, 404, ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<GenericResponse> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entidad JPA no encontrada: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, 404, "El recurso solicitado no fue encontrado.");
    }

    @ExceptionHandler({
            ClienteYaExisteException.class,
            CurpDuplicadaException.class,
            RfcDuplicadoException.class,
            UsuarioYaExisteException.class,
            ClienteYaEliminadoException.class
    })
    public ResponseEntity<GenericResponse> handleConflictosExistencia(RuntimeException ex) {
        log.warn("Conflicto de estado o duplicidad en la entidad: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, 409, ex.getMessage());
    }

    @ExceptionHandler({CredencialesInvalidasException.class, BadCredentialsException.class, SesionExpiradaException.class})
    public ResponseEntity<GenericResponse> handleAutenticacion(RuntimeException ex) {
        log.warn("Falla de autenticación/sesión: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, 401, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse> handleAccesoDenegado(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, 403, "No cuenta con los permisos necesarios para acceder a este recurso.");
    }

    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<GenericResponse> handleValidacionNegocio(ValidacionNegocioException ex) {
        log.warn("Falla de regla de negocio: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, 400, ex.getMessage());
    }

    @ExceptionHandler(CuentaNoAutorizadaException.class)
    public ResponseEntity<GenericResponse> handleCuentaNoAutorizada(CuentaNoAutorizadaException ex) {
        log.warn("Cuenta no autorizada: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, 403, ex.getMessage());
    }

    @ExceptionHandler(SegundoFactorInvalidoException.class)
    public ResponseEntity<GenericResponse> handleSegundoFactorInvalido(SegundoFactorInvalidoException ex) {
        log.warn("Segunda autenticación rechazada");
        return buildResponse(HttpStatus.UNAUTHORIZED, 401, ex.getMessage());
    }

    @ExceptionHandler(ProductoNoValidoException.class)
    public ResponseEntity<GenericResponse> handleProductoNoValido(ProductoNoValidoException ex) {
        log.warn("Producto/servicio no válido: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, 400, ex.getMessage());
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<GenericResponse> handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        log.warn("Saldo insuficiente");
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, 422, ex.getMessage());
    }

    @ExceptionHandler(IntegracionNoDisponibleException.class)
    public ResponseEntity<GenericResponse> handleIntegracionNoDisponible(IntegracionNoDisponibleException ex) {
        log.error("Integración no disponible: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, 503, ex.getMessage());
    }

    @ExceptionHandler(OperacionDuplicadaException.class)
    public ResponseEntity<GenericResponse> handleOperacionDuplicada(OperacionDuplicadaException ex) {
        log.warn("Operación duplicada detectada: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, 409, ex.getMessage());
    }

    @ExceptionHandler(CifradoException.class)
    public ResponseEntity<GenericResponse> handleCifradoException(CifradoException ex) {
        log.error("Error criptográfico en la operación: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, 500, "Error en el procesamiento seguro de la transacción.");
    }

    @ExceptionHandler(XmlParsingException.class)
    public ResponseEntity<GenericResponse> handleXmlParsingException(XmlParsingException ex) {
        log.error("Error al procesar respuesta XML del proveedor: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, 502, "Respuesta con formato no válido del proveedor externo.");
    }

    @ExceptionHandler(ProveedorPagoException.class)
    public ResponseEntity<GenericResponse> handleProveedorPago(ProveedorPagoException ex) {
        log.warn("Rechazo del proveedor de pagos: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, 422, ex.getMessage());
    }

    /**
     * Estandarización de validaciones DTO al estilo ValidadorCampos.
     * Retorna ResponseEntity<GenericResponse> con HttpStatus.BAD_REQUEST,
     * codigo 1 y mensaje: "Datos no validos: " + listError.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> dtoValidation(MethodArgumentNotValidException exception) {
        GenericResponse genericResponse = new GenericResponse();
        Map<String, String> listError = new HashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            listError.merge(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Dato no válido",
                    (m1, m2) -> m1 + "; " + m2
            );
        }

        for (ObjectError globalError : exception.getBindingResult().getGlobalErrors()) {
            listError.merge(
                    globalError.getObjectName(),
                    globalError.getDefaultMessage() != null ? globalError.getDefaultMessage() : "Dato no válido",
                    (m1, m2) -> m1 + "; " + m2
            );
        }

        genericResponse.setCodigo(1);
        genericResponse.setMensaje("Datos no validos: " + listError);

        log.warn("Falla de validación en Request DTO: {}", genericResponse.getMensaje());
        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Solicitud JSON mal formada o no legible: {}", ex.getMessage());
        String mensaje = "El cuerpo de la solicitud JSON es inválido o contiene tipos de datos incorrectos.";

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse("solicitud");
            mensaje = "El campo '" + fieldName + "' tiene un formato o tipo de dato inválido: '" + ife.getValue() + "'.";
        } else if (cause instanceof MismatchedInputException mie) {
            String fieldName = mie.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse("solicitud");
            mensaje = "El tipo de dato proporcionado para el campo '" + fieldName + "' es inválido.";
        } else if (cause instanceof JsonParseException) {
            mensaje = "Sintaxis JSON inválida o mal formada. Verifique las comas, comillas y llaves del JSON.";
        }

        return buildResponse(HttpStatus.BAD_REQUEST, 400, mensaje);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Violación de restricciones de validación: {}", ex.getMessage());
        GenericResponse genericResponse = new GenericResponse();
        Map<String, String> listError = new HashMap<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String property = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "parametro";
            listError.merge(
                    property,
                    violation.getMessage() != null ? violation.getMessage() : "Dato no válido",
                    (m1, m2) -> m1 + "; " + m2
            );
        }

        genericResponse.setCodigo(1);
        genericResponse.setMensaje("Datos no validos: " + listError);

        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Violación de integridad de datos en BD: {}", ex.getMessage());
        return buildResponse(
                HttpStatus.CONFLICT,
                409,
                "Conflicto de integridad: ya existe un registro con información duplicada o incompatible en el sistema."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenericException(Exception ex) {
        log.error("Excepción no controlada en el sistema: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, 500, "Ocurrió un error inesperado al procesar la solicitud.");
    }

    private ResponseEntity<GenericResponse> buildResponse(HttpStatus status, Integer codigo, String mensaje) {
        GenericResponse genericResponse = GenericResponse.builder()
                .codigo(codigo)
                .mensaje(mensaje)
                .build();
        return new ResponseEntity<>(genericResponse, status);
    }
}
