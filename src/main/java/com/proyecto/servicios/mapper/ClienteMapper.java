package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.entity.Cliente;

/**
 * Mapper para convertir entre ClienteRequestDTO / Cliente entity y ClienteResponseDTO.
 */
public class ClienteMapper {

    public static Cliente toEntity(ClienteRequestDTO dto) {
        if (dto == null) return null;
        return Cliente.builder()
                .nombre(dto.getNombre())
                .segundoNombre(dto.getSegundoNombre())
                .apellidoPaterno(dto.getApellidoPaterno())
                .apellidoMaterno(dto.getApellidoMaterno())
                .fechaNacimiento(dto.getFechaNacimiento())
                .curp(dto.getCurp())
                .rfc(dto.getRfc())
                .sexo(dto.getSexo())
                .nacionalidad(dto.getNacionalidad() != null ? dto.getNacionalidad() : "MEXICANA")
                .estadoCivil(dto.getEstadoCivil())
                .correo(dto.getCorreo())
                .movil(dto.getMovil())
                .telefonoAlternativo(dto.getTelefonoAlternativo())
                .ocupacion(dto.getOcupacion())
                .empresa(dto.getEmpresa())
                .ingresoMensual(dto.getIngresoMensual())
                .activo(true)
                .eliminado(false)
                .build();
    }

    public static ClienteResponseDTO toDTO(Cliente entity) {
        if (entity == null) return null;
        return ClienteResponseDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .segundoNombre(entity.getSegundoNombre())
                .apellidoPaterno(entity.getApellidoPaterno())
                .apellidoMaterno(entity.getApellidoMaterno())
                .fechaNacimiento(entity.getFechaNacimiento())
                .curp(entity.getCurp())
                .rfc(entity.getRfc())
                .sexo(entity.getSexo())
                .nacionalidad(entity.getNacionalidad())
                .estadoCivil(entity.getEstadoCivil())
                .correo(entity.getCorreo())
                .movil(entity.getMovil())
                .telefonoAlternativo(entity.getTelefonoAlternativo())
                .ocupacion(entity.getOcupacion())
                .empresa(entity.getEmpresa())
                .ingresoMensual(entity.getIngresoMensual())
                .activo(entity.getActivo())
                .eliminado(entity.getEliminado())
                .fechaBaja(entity.getFechaBaja())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .domicilio(DomicilioMapper.toDTO(entity.getDomicilio()))
                .build();
    }
}
