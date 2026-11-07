package com.proyecto.servicios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transferencia de datos de Domicilio con validaciones complejas de entrada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomicilioDTO {

    private Long id;

    @NotBlank(message = "La calle es obligatoria.")
    @Size(min = 2, max = 255, message = "La calle debe contener entre 2 y 255 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s\\.\\,#\\-]+$", message = "La calle contiene caracteres inválidos.")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio.")
    @Size(min = 1, max = 20, message = "El número exterior debe tener entre 1 y 20 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-\\/#]+$", message = "El número exterior contiene caracteres inválidos.")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no puede exceder 20 caracteres.")
    @Pattern(regexp = "^(?:$|[a-zA-Z0-9\\s\\-\\/#]+)$", message = "El número interior contiene caracteres inválidos.")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria.")
    @Size(min = 2, max = 255, message = "La colonia debe contener entre 2 y 255 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s\\.\\,#\\-]+$", message = "La colonia contiene caracteres inválidos.")
    private String colonia;

    @NotBlank(message = "El municipio o alcaldía es obligatorio.")
    @Size(min = 2, max = 255, message = "El municipio debe contener entre 2 y 255 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s\\.\\-]+$", message = "El municipio solo debe contener letras y espacios.")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio.")
    @Size(min = 2, max = 255, message = "El estado debe contener entre 2 y 255 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s\\.\\-]+$", message = "El estado solo debe contener letras y espacios.")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio.")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos.")
    private String codigoPostal;

    @Size(min = 2, max = 100, message = "El país debe contener entre 2 y 100 caracteres.")
    @Pattern(regexp = "^(?:$|[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+)$", message = "El país solo debe contener letras y espacios.")
    @Builder.Default
    private String pais = "MÉXICO";
}
