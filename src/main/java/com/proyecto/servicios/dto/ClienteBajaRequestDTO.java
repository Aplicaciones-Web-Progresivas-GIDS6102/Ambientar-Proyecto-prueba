package com.proyecto.servicios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de solicitud para la operación de baja lógica de un cliente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para ejecutar la baja lógica de un cliente")
public class ClienteBajaRequestDTO {

    @NotNull(message = "El identificador del cliente es obligatorio.")
    @Positive(message = "El identificador del cliente debe ser un número positivo mayor a cero.")
    @Schema(description = "Identificador único del cliente a dar de baja", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Size(max = 255, message = "El motivo de la baja no puede exceder 255 caracteres.")
    @Schema(description = "Motivo opcional de la baja lógica para auditoría", example = "Cancelación voluntaria solicitada por el titular")
    private String motivo;
}
