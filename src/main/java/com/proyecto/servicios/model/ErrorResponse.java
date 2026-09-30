package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO estándar y reutilizable para la transferencia de detalles de error en la API REST.
 * 
 * Justificación Técnica: Permite responder con códigos HTTP semánticos (400, 404, 500, etc.)
 * para la futura arquitectura bancaria, manteniendo estructura no nula y sellos de tiempo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private String codigo = "500";

    @Builder.Default
    private String mensaje = "Ocurrió un error al procesar la solicitud.";

    private Integer estado;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private List<String> detalles;
}
