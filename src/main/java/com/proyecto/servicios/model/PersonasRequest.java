package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonasRequest {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(min = 2, max = 50, message = "El nombre debe contener entre 2 y 50 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El nombre solo debe contener letras y espacios.")
    private String nombre;

    @NotBlank(message = "El apellido paterno es obligatorio.")
    @Size(min = 2, max = 50, message = "El apellido paterno debe contener entre 2 y 50 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios.")
    private String apellidoP;

    @NotBlank(message = "El apellido materno es obligatorio.")
    @Size(min = 2, max = 50, message = "El apellido materno debe contener entre 2 y 50 caracteres.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido materno solo debe contener letras y espacios.")
    private String apellidoMaterno;

}
