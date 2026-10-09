package com.proyecto.servicios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta con la información de una Cuenta Bancaria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaResponseDTO {

    private Long id;
    private Long clienteId;
    private String numeroCuenta;
    private EstatusCuentaDTO estatusCuenta;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    private SaldoDTO saldo;
}
