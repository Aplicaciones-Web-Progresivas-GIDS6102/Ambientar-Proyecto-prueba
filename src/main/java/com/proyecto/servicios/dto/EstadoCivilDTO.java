package com.proyecto.servicios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCivilDTO {
    private Long id;
    private String descripcion;
    private Boolean activo;
}
