package com.proyecto.servicios.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para la transferencia de información de Saldo de una Cuenta Bancaria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaldoDTO {

    private Long id;
    private Long cuentaId;

    @NotNull(message = "El saldo disponible es obligatorio.")
    @DecimalMin(value = "0.00", message = "El saldo disponible no puede ser negativo.")
    private BigDecimal saldoDisponible;

    @NotNull(message = "El saldo contable es obligatorio.")
    @DecimalMin(value = "0.00", message = "El saldo contable no puede ser negativo.")
    private BigDecimal saldoContable;

    @Builder.Default
    @Size(max = 3, message = "La moneda debe ser un código ISO de 3 caracteres.")
    private String moneda = "MXN";

    private LocalDateTime fechaActualizacion;
}
