package com.proyecto.servicios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta con la información de Usuario.
 * OMITIENDO estrictamente la contraseña y el hash por motivos de seguridad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private Long id;
    private Long clienteId;
    private String username;
    private Boolean activo;
    private Integer intentosFallidos;
    private Boolean bloqueado;
    private LocalDateTime fechaUltimoAcceso;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
