package com.proyecto.servicios.config;

import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;


 // Manejador global de excepciones centralizado para interceptar errores de validación de request
 // y fallos inesperados en la aplicación.

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ConsultaProductosResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder();
        
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(fieldError.getDefaultMessage());
        }

        String mensajeError = sb.length() > 0 ? sb.toString() : "Error en los parámetros de entrada de la petición.";
        log.warn("Falla de validación en Request: {}", mensajeError);

        ConsultaProductosResponse response = ConsultaProductosResponse.builder()
                .codigo("400")
                .mensaje(mensajeError)
                .productos(new ArrayList<>())
                .build();

        // Retorna HTTP 200 OK con el cuerpo conteniendo el detalle del error de validación
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    //Manejador genérico para capturar cualquier excepción no controlada.

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ConsultaProductosResponse> handleGenericException(Exception ex) {
        log.error("Excepción no controlada en el sistema: {}", ex.getMessage(), ex);

        ConsultaProductosResponse response = ConsultaProductosResponse.builder()
                .codigo("500")
                .mensaje("Ocurrió un error inesperado al procesar la solicitud.")
                .productos(new ArrayList<>())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
