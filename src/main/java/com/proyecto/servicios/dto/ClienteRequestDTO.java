package com.proyecto.servicios.dto;

import com.proyecto.servicios.enums.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para la solicitud de registro / alta de Cliente en Onboarding con validaciones Bean Validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequestDTO {

    @NotBlank(message = "El primer nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres.")
    private String nombre;

    @Size(max = 100, message = "El segundo nombre no debe exceder 100 caracteres.")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio.")
    @Size(max = 100, message = "El apellido paterno no debe exceder 100 caracteres.")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio.")
    @Size(max = 100, message = "El apellido materno no debe exceder 100 caracteres.")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria.")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada.")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria.")
    @Size(min = 18, max = 18, message = "La CURP debe contener exactamente 18 caracteres.")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "El formato de la CURP es inválido.")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio.")
    @Size(min = 12, max = 13, message = "El RFC debe contener entre 12 y 13 caracteres.")
    @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El formato del RFC es inválido.")
    private String rfc;

    @NotNull(message = "El sexo es obligatorio.")
    private Sexo sexo;

    @Builder.Default
    private String nacionalidad = "MEXICANA";

    private String estadoCivil;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico debe ser una dirección válida.")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres.")
    private String correo;

    @NotBlank(message = "El número móvil es obligatorio.")
    @Pattern(regexp = "\\d{10}", message = "El número móvil debe contener exactamente 10 dígitos numéricos.")
    private String movil;

    @Pattern(regexp = "^$|\\d{10,15}", message = "El teléfono alternativo debe ser numérico entre 10 y 15 dígitos.")
    private String telefonoAlternativo;

    private String ocupacion;

    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio.")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor que cero.")
    private BigDecimal ingresoMensual;

    @NotNull(message = "Los datos del domicilio son obligatorios.")
    @Valid
    private DomicilioDTO domicilio;
}
