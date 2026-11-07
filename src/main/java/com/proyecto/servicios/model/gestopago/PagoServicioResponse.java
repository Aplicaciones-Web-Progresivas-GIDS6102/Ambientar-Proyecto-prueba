package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.EstadoPagoServicio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resultado interno de un pago de servicio. Solo APLICADO indica pago confirmado por el proveedor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagoServicioResponse {
    private EstadoPagoServicio estado;
    private String upc;
    private String idServicio;
    private String idProducto;
    private BigDecimal monto;
    private String mensaje;
}
