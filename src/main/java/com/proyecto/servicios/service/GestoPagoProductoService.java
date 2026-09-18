package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;


// Interfaz del servicio de negocio para la gestión y consulta de productos de GestoPago.

public interface GestoPagoProductoService {

    // Obtiene la lista de productos disponibles desde el servicio externo de GestoPago.
    //@param request Petición con datos de entrada validados.
    //@return ConsultaProductosResponse DTO con el resultado de la operación y la lista de productos sin valores nulos.
     ConsultaProductosResponse obtenerProductos(ConsultaProductosRequest request);
}
