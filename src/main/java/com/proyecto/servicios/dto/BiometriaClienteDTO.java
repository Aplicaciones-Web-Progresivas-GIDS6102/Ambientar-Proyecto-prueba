package com.proyecto.servicios.dto;

import com.proyecto.servicios.enums.TipoBiometria;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para la transferencia de datos de Muestras Biométricas de Cliente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometriaClienteDTO {

    private Long id;

    @NotNull(message = "El ID del cliente es obligatorio.")
    private Long clienteId;

    @NotNull(message = "El tipo de biometría es obligatorio.")
    private TipoBiometria tipoBiometria;

    private String vectorCaracteristicas;
    private String hashBiometrico;
    private String algoritmo;
    private BigDecimal puntuacionCalidad;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}
