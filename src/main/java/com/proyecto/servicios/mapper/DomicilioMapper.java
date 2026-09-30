package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.DomicilioDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Domicilio;

/**
 * Mapper para convertir entre DomicilioDTO y Domicilio entity.
 */
public class DomicilioMapper {

    public static Domicilio toEntity(DomicilioDTO dto, Cliente cliente) {
        if (dto == null) return null;
        return Domicilio.builder()
                .id(dto.getId())
                .cliente(cliente)
                .calle(dto.getCalle())
                .numeroExterior(dto.getNumeroExterior())
                .numeroInterior(dto.getNumeroInterior())
                .colonia(dto.getColonia())
                .municipio(dto.getMunicipio())
                .estado(dto.getEstado())
                .codigoPostal(dto.getCodigoPostal())
                .pais(dto.getPais() != null ? dto.getPais() : "MÉXICO")
                .build();
    }

    public static DomicilioDTO toDTO(Domicilio entity) {
        if (entity == null) return null;
        return DomicilioDTO.builder()
                .id(entity.getId())
                .calle(entity.getCalle())
                .numeroExterior(entity.getNumeroExterior())
                .numeroInterior(entity.getNumeroInterior())
                .colonia(entity.getColonia())
                .municipio(entity.getMunicipio())
                .estado(entity.getEstado())
                .codigoPostal(entity.getCodigoPostal())
                .pais(entity.getPais())
                .build();
    }
}
