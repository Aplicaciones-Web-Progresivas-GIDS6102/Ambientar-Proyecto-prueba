package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Modelo de respuesta genérico y estandarizado para la API REST.
 * Respeta el contrato del sistema: codigo (Integer), mensaje (String) y data opcional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer codigo;
    private String mensaje;
    private Object data;

    public GenericResponse(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }
}
