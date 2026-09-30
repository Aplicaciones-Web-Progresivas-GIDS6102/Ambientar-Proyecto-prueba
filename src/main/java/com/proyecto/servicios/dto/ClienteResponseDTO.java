package com.proyecto.servicios.dto;

import com.proyecto.servicios.enums.Sexo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para la respuesta con la información completa de un Cliente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {

    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private Sexo sexo;
    private String nacionalidad;
    private String estadoCivil;
    private String correo;
    private String movil;
    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private Boolean eliminado;
    private LocalDateTime fechaBaja;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    private DomicilioDTO domicilio;
}
