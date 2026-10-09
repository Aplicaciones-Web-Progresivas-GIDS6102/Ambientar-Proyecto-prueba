package com.proyecto.servicios.dto;

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
    @Size(min = 2, max = 50, message = "El nombre debe contener entre 2 y 50 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El nombre solo debe contener letras y espacios.")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no puede exceder 50 caracteres.")
    @Pattern(regexp = "^(?:$|[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{2,50})$", message = "El segundo nombre solo debe contener letras y espacios, con entre 2 y 50 caracteres si se proporciona.")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio.")
    @Size(min = 2, max = 50, message = "El apellido paterno debe contener entre 2 y 50 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios.")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio.")
    @Size(min = 2, max = 50, message = "El apellido materno debe contener entre 2 y 50 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido materno solo debe contener letras y espacios.")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria y debe ser una fecha válida (AAAA-MM-DD).")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada.")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.proyecto.servicios.config.FlexibleLocalDateDeserializer.class)
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria.")
    @Size(min = 18, max = 18, message = "La CURP debe contener exactamente 18 caracteres.")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "El formato de la CURP es inválido.")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio.")
    @Pattern(
            regexp = "^(?:$|[A-ZÑ&]{3}\\d{6}[A-Z0-9]{3}|[A-ZÑ&]{4}\\d{6}[A-Z0-9]{3})$",
            message = "El RFC debe incluir homoclave: 12 caracteres para persona moral o 13 para persona física."
    )
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio.")
    @Pattern(regexp = "^(?i)(M|F|X|HOMBRE|MUJER|MASCULINO|FEMENINO|H|M)$", message = "El sexo es inválido. Debe ser M, F o X.")
    private String sexo;

    private Long nacionalidadId;

    @Builder.Default
    private String nacionalidad = "MEXICANA";

    private Long estadoCivilId;
    private String estadoCivil;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico debe ser una dirección válida.")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres.")
    private String correo;

    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "El móvil solo debe contener números (entre 10 y 15 dígitos).")
    private String movil;

    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "El teléfono móvil solo debe contener números (entre 10 y 15 dígitos).")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "El teléfono alternativo solo debe contener números (entre 10 y 15 dígitos).")
    private String telefonoAlternativo;

    // Campos de información laboral (flat o nested)
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;

    @Valid
    private InformacionLaboralDTO informacionLaboral;

    @NotNull(message = "Los datos del domicilio son obligatorios.")
    @Valid
    private DomicilioDTO domicilio;

    // Campos opcionales para credenciales de onboarding si aplican
    @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres.")
    private String username;

    @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres.")
    private String password;

    public String getTelefonoMovilFinal() {
        if (telefonoMovil != null && !telefonoMovil.isBlank()) return telefonoMovil;
        return movil;
    }
}
