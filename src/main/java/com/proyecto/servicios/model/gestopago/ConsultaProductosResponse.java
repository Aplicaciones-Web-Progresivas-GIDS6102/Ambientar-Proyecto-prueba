package com.proyecto.servicios.model.gestopago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO para la respuesta estandarizada de la API de Productos.
 * Asegura que NINGÚN campo sea devuelto como nulo (No nulos),
 * inicializando siempre las listas y las cadenas con valores por defecto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaProductosResponse {

    //Código de respuesta del negocio (ej. "01" Éxito, "400" Error de validación, "500" Error interno).

    @Builder.Default
    private String codigo = "01";

     //Descripción clara del resultado de la operación o mensaje del error.

    @Builder.Default
    private String mensaje = "Operacion realizada con exito";

    //Lista de productos recuperados. Nunca será null (se inicializa como lista vacía).
    @Builder.Default
    private List<ProductoDto> productos = new ArrayList<>();
}
