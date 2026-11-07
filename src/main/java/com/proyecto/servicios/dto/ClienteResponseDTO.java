package com.proyecto.servicios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO para la respuesta con la información completa de un Cliente, sus Cuentas, Domicilio e Información Laboral.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    
    private PaisDTO nacionalidad;
    private EstadoCivilDTO estadoCivil;

    private String correo;
    private String telefonoMovil;
    private String movil; // alias para compatibilidad JSON
    private String telefonoAlternativo;

    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private InformacionLaboralDTO informacionLaboral;

    private Boolean activo;
    private LocalDateTime fechaBaja;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    private DomicilioDTO domicilio;

    @Builder.Default
    private List<CuentaResponseDTO> cuentas = new ArrayList<>();
}
