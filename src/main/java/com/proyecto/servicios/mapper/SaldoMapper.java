package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.SaldoDTO;
import com.proyecto.servicios.entity.Saldo;

/**
 * Mapper para convertir entre SaldoDTO y Saldo entity.
 */
public class SaldoMapper {

    public static SaldoDTO toDTO(Saldo entity) {
        if (entity == null) return null;
        return SaldoDTO.builder()
                .id(entity.getId())
                .cuentaId(entity.getCuenta() != null ? entity.getCuenta().getId() : null)
                .saldoDisponible(entity.getSaldoDisponible())
                .saldoContable(entity.getSaldoContable())
                .moneda(entity.getMoneda())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}
