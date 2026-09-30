package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.CuentaResponseDTO;
import com.proyecto.servicios.entity.Cuenta;

/**
 * Mapper para convertir entre Cuenta entity y CuentaResponseDTO.
 */
public class CuentaMapper {

    public static CuentaResponseDTO toDTO(Cuenta entity) {
        if (entity == null) return null;
        return CuentaResponseDTO.builder()
                .id(entity.getId())
                .clienteId(entity.getCliente() != null ? entity.getCliente().getId() : null)
                .numeroCuenta(entity.getNumeroCuenta())
                .clabe(entity.getClabe())
                .tipoCuenta(entity.getTipoCuenta())
                .estado(entity.getEstado())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .saldo(SaldoMapper.toDTO(entity.getSaldo()))
                .build();
    }
}
