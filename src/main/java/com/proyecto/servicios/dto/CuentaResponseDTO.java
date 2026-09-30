package com.proyecto.servicios.dto;

import com.proyecto.servicios.enums.EstadoCuenta;
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
    private String clabe;
    private String tipoCuenta;
    private EstadoCuenta estado;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    private SaldoDTO saldo;
}
