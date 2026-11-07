package com.proyecto.servicios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta para la consulta de confirmación / conciliación de una transacción de pago.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfirmacionTxResponseDTO {

    private String upc;
    private EstadoPagoServicio estado;
    private String codigoProveedor;
    private String mensaje;
    private BigDecimal monto;
    private String idServicio;
    private String idProducto;
    private boolean saldoDescontado;
}
