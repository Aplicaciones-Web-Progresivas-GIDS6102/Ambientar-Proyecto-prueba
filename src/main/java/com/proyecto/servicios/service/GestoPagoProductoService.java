package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;

/**
 * Interfaz de la capa de negocio para la gestión, consulta y sincronización de productos de GestoPago.
 */
public interface GestoPagoProductoService {

    /**
     * Obtiene la lista de productos disponibles. Prioriza la caché en Redis y recurre a la BD PostgreSQL si falla.
     */
    ConsultaProductosResponse obtenerProductos(ConsultaProductosRequest request);

    /**
     * Sincroniza la lista de productos con el proveedor externo y la ordena por tipoFront (menor a mayor).
     */
    ConsultaProductosResponse sincronizarProductos();
}
