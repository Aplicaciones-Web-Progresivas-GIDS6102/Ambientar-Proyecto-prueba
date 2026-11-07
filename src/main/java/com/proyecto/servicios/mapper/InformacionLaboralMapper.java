package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.InformacionLaboralDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.InformacionLaboral;

/**
 * Mapper para convertir entre InformacionLaboralDTO y InformacionLaboral entity.
 */
public class InformacionLaboralMapper {

    public static InformacionLaboral toEntity(InformacionLaboralDTO dto, Cliente cliente) {
        if (dto == null) return null;
        return InformacionLaboral.builder()
                .id(dto.getId())
                .cliente(cliente)
                .ocupacion(dto.getOcupacion())
                .empresa(dto.getEmpresa())
                .ingresoMensual(dto.getIngresoMensual())
                .build();
    }

    public static InformacionLaboralDTO toDTO(InformacionLaboral entity) {
        if (entity == null) return null;
        return InformacionLaboralDTO.builder()
                .id(entity.getId())
                .ocupacion(entity.getOcupacion())
                .empresa(entity.getEmpresa())
                .ingresoMensual(entity.getIngresoMensual())
                .build();
    }
}
