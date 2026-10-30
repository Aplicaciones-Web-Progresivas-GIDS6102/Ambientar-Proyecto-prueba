package com.proyecto.servicios.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la consulta dinámica de clientes mediante criterios combinables.
 * Todos los campos son opcionales. Cuando no se envía ningún criterio,
 * la consulta devuelve todos los usuarios activos excluyendo los dados de baja.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Criterios combinables para la búsqueda de clientes activos")
public class ClienteFiltroRequestDTO {

    @Size(max = 100, message = "El filtro de nombre no puede superar los 100 caracteres.")
    @Schema(description = "Búsqueda parcial en nombre, segundo nombre o apellidos", example = "Carlos")
    private String nombre;

    @Pattern(
            regexp = "^$|^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$",
            message = "El formato de la CURP en el filtro es inválido. Debe contener 18 caracteres alfanuméricos válidos."
    )
    @Schema(description = "CURP exacta del cliente", example = "HERC900515HMCRGR01")
    private String curp;

    @Pattern(
            regexp = "^$|^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$",
            message = "El formato del RFC en el filtro es inválido. Debe contener 12 o 13 caracteres con homoclave."
    )
    @Schema(description = "RFC exacto del cliente", example = "HERC900515XX1")
    private String rfc;

    @Email(message = "El formato del correo electrónico en el filtro es inválido.")
    @Size(max = 100, message = "El correo electrónico no puede superar los 100 caracteres.")
    @Schema(description = "Correo electrónico exacto del cliente", example = "carlos.hernandez@email.com")
    private String correo;

    @Schema(description = "Fecha de inicio del rango de creación (AAAA-MM-DDTHH:MM:SS)", example = "2026-01-01T00:00:00")
    private LocalDateTime fechaCreacionDesde;

    @Schema(description = "Fecha final del rango de creación (AAAA-MM-DDTHH:MM:SS)", example = "2026-12-31T23:59:59")
    private LocalDateTime fechaCreacionHasta;

    @AssertTrue(message = "La fecha de inicio (fechaCreacionDesde) no puede ser posterior a la fecha de fin (fechaCreacionHasta).")
    @JsonIgnore
    public boolean isRangoFechasValido() {
        if (fechaCreacionDesde != null && fechaCreacionHasta != null) {
            return !fechaCreacionDesde.isAfter(fechaCreacionHasta);
        }
        return true;
    }

    @JsonIgnore
    public boolean tieneFiltros() {
        return (nombre != null && !nombre.isBlank())
                || (curp != null && !curp.isBlank())
                || (rfc != null && !rfc.isBlank())
                || (correo != null && !correo.isBlank())
                || fechaCreacionDesde != null
                || fechaCreacionHasta != null;
    }
}
