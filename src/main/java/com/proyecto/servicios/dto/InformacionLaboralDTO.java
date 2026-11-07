package com.proyecto.servicios.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformacionLaboralDTO {

    private Long id;

    @NotBlank(message = "La ocupación es obligatoria.")
    @Size(min = 2, max = 100, message = "La ocupación debe contener entre 2 y 100 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s\\.\\-]+$", message = "La ocupación solo debe contener letras y espacios.")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria.")
    @Size(min = 2, max = 100, message = "La empresa debe contener entre 2 y 100 caracteres.")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio.")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor que cero.")
    private BigDecimal ingresoMensual;
}
