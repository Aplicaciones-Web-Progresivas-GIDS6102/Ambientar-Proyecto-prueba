package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.*;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.EstadoCivil;
import com.proyecto.servicios.entity.InformacionLaboral;
import com.proyecto.servicios.entity.Pais;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre ClienteRequestDTO / Cliente entity y ClienteResponseDTO.
 * Implementa sobrecarga de métodos en toDTO para entidades individuales y colecciones.
 */
public class ClienteMapper {

    public static Cliente toEntity(ClienteRequestDTO dto, Pais nacionalidad, EstadoCivil estadoCivil) {
        if (dto == null) return null;

        String movilFinal = dto.getTelefonoMovilFinal();

        String segNombre = (dto.getSegundoNombre() != null && !dto.getSegundoNombre().isBlank())
                ? dto.getSegundoNombre().trim()
                : null;

        Cliente cliente = Cliente.builder()
                .nombre(dto.getNombre())
                .segundoNombre(segNombre)
                .apellidoPaterno(dto.getApellidoPaterno())
                .apellidoMaterno(dto.getApellidoMaterno())
                .fechaNacimiento(dto.getFechaNacimiento())
                .curp(dto.getCurp())
                .rfc(dto.getRfc())
                .sexo(dto.getSexo())
                .nacionalidad(nacionalidad)
                .estadoCivil(estadoCivil)
                .correo(dto.getCorreo())
                .telefonoMovil(movilFinal)
                .telefonoAlternativo(dto.getTelefonoAlternativo())
                .activo(true)
                .build();

        // Mapear Información Laboral si está presente
        if (dto.getInformacionLaboral() != null) {
            InformacionLaboral infLab = InformacionLaboralMapper.toEntity(dto.getInformacionLaboral(), cliente);
            cliente.setInformacionLaboral(infLab);
        } else if (dto.getOcupacion() != null || dto.getEmpresa() != null || dto.getIngresoMensual() != null) {
            InformacionLaboral infLab = InformacionLaboral.builder()
                    .cliente(cliente)
                    .ocupacion(dto.getOcupacion() != null ? dto.getOcupacion() : "NO ESPECIFICADO")
                    .empresa(dto.getEmpresa() != null ? dto.getEmpresa() : "NO ESPECIFICADO")
                    .ingresoMensual(dto.getIngresoMensual() != null ? dto.getIngresoMensual() : java.math.BigDecimal.ZERO)
                    .build();
            cliente.setInformacionLaboral(infLab);
        }

        return cliente;
    }

    /**
     * Sobrecarga 1: Convierte una entidad Cliente a ClienteResponseDTO.
     */
    public static ClienteResponseDTO toDTO(Cliente entity) {
        if (entity == null) return null;

        PaisDTO paisDTO = null;
        if (entity.getNacionalidad() != null) {
            paisDTO = PaisDTO.builder()
                    .id(entity.getNacionalidad().getId())
                    .nombre(entity.getNacionalidad().getNombre())
                    .codigoIso(entity.getNacionalidad().getCodigoIso())
                    .activo(entity.getNacionalidad().getActivo())
                    .build();
        }

        EstadoCivilDTO estadoCivilDTO = null;
        if (entity.getEstadoCivil() != null) {
            estadoCivilDTO = EstadoCivilDTO.builder()
                    .id(entity.getEstadoCivil().getId())
                    .descripcion(entity.getEstadoCivil().getDescripcion())
                    .activo(entity.getEstadoCivil().getActivo())
                    .build();
        }

        InformacionLaboralDTO infLabDTO = InformacionLaboralMapper.toDTO(entity.getInformacionLaboral());

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
                .nacionalidad(paisDTO)
                .estadoCivil(estadoCivilDTO)
                .correo(entity.getCorreo())
                .telefonoMovil(entity.getTelefonoMovil())
                .movil(entity.getTelefonoMovil())
                .telefonoAlternativo(entity.getTelefonoAlternativo())
                .ocupacion(infLabDTO != null ? infLabDTO.getOcupacion() : null)
                .empresa(infLabDTO != null ? infLabDTO.getEmpresa() : null)
                .ingresoMensual(infLabDTO != null ? infLabDTO.getIngresoMensual() : null)
                .informacionLaboral(infLabDTO)
                .activo(entity.getActivo())
                .fechaBaja(entity.getFechaBaja())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .domicilio(DomicilioMapper.toDTO(entity.getDomicilio()))
                .cuentas(entity.getCuentas() != null ? entity.getCuentas().stream()
                        .map(CuentaMapper::toDTO)
                        .collect(Collectors.toList()) : java.util.Collections.emptyList())
                .build();
    }

    /**
     * Sobrecarga 2: Convierte una lista de entidades Cliente a lista de ClienteResponseDTO.
     * Reutiliza la sobrecarga individual toDTO(Cliente).
     */
    public static List<ClienteResponseDTO> toDTO(List<Cliente> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(ClienteMapper::toDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
