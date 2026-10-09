package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.CuentaResponseDTO;
import com.proyecto.servicios.dto.EstatusCuentaDTO;
import com.proyecto.servicios.entity.Cuenta;

/**
 * Mapper para convertir entre Cuenta entity y CuentaResponseDTO.
 */
public class CuentaMapper {

    public static CuentaResponseDTO toDTO(Cuenta entity) {
        if (entity == null) return null;

        EstatusCuentaDTO estatusDTO = null;
        if (entity.getEstatusCuenta() != null) {
            estatusDTO = EstatusCuentaDTO.builder()
                    .id(entity.getEstatusCuenta().getId())
                    .nombre(entity.getEstatusCuenta().getNombre())
                    .descripcion(entity.getEstatusCuenta().getDescripcion())
                    .activo(entity.getEstatusCuenta().getActivo())
                    .build();
        }

        return CuentaResponseDTO.builder()
                .id(entity.getId())
                .clienteId(entity.getCliente() != null ? entity.getCliente().getId() : null)
                .numeroCuenta(entity.getNumeroCuenta())
                .estatusCuenta(estatusDTO)
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .saldo(SaldoMapper.toDTO(entity.getSaldo()))
                .build();
    }
}
