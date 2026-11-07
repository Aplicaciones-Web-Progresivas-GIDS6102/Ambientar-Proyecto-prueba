package com.proyecto.servicios.model.gestopago;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//DTO para la petición de consulta de productos.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaProductosRequest {

    @NotBlank(message = "El usuario es obligatorio y no puede estar vacío.")
    @Builder.Default
    private String usuario = "";


    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 7, message = "La contraseña debe tener al menos 7 caracteres.")
    @Builder.Default
    private String password = "";
}
