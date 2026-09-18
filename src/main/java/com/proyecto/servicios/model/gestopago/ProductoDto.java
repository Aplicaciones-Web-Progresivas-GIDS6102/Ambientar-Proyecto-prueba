package com.proyecto.servicios.model.gestopago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO limpio de negocio que representa un producto de GestoPago.
 * Garantiza que todos sus campos tengan valores válidos por defecto (evitando nulos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDto {

    @Builder.Default
    private String servicio = "";

    @Builder.Default
    private String producto = "";

    @Builder.Default
    private String idServicio = "";

    @Builder.Default
    private String idProducto = "";

    @Builder.Default
    private String idCatTipoServicio = "";

    @Builder.Default
    private String tipoFront = "";

    @Builder.Default
    private Boolean hasDigitoVerificador = false;

    @Builder.Default
    private Double precio = 0.0;

    @Builder.Default
    private Boolean showAyuda = false;

    @Builder.Default
    private String tipoReferencia = "";

    @Builder.Default
    private String legend = "";
}
