package com.proyecto.servicios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la transferencia de información sobre las Sesiones de Usuario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionDTO {

    private Long id;
    private Long usuarioId;
    private String tokenSesion;
    private Boolean activa;
    private String ipOrigen;
    private String userAgent;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaUltimaActividad;
    private LocalDateTime fechaExpiracion;
}
