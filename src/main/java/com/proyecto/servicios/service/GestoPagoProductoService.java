package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.ConsultaProductosRequest;
import com.proyecto.servicios.model.gestopago.ConsultaProductosResponse;

/**
 * Interfaz de la capa de negocio para la gestión, consulta y sincronización de productos de GestoPago.
 * 
 * ¿QUÉ HACE?
 * Expone las operaciones para obtener el catálogo de productos con almacenamiento en Redis,
 * respaldo en PostgreSQL para modo offline y sincronización programada por Cron.
 * 
 * ¿CÓMO LO HACE?
 * - {@link #obtenerProductos(ConsultaProductosRequest)}: Consulta primero la caché en Redis; si no existe,
 *   invoca el proveedor web externo. Si responde 200 OK, borra los anteriores de la BD e inserta los nuevos;
 *   si no hay Internet, recupera los datos previamente respaldados en PostgreSQL.
 * - {@link #sincronizarProductos()}: Fuerza la invocación al servicio externo (ejecutado a las 6:00 AM por el Cron),
 *   actualizando la BD PostgreSQL (eliminando los anteriores solo tras obtener 200 OK) y refrescando la caché Redis.
 */
public interface GestoPagoProductoService {

    /**
     * Obtiene la lista de productos disponibles. Prioriza la caché en Redis y recurre a la BD PostgreSQL
     * si el servicio externo no está disponible.
     *
     * @param request Petición con datos de entrada.
     * @return ConsultaProductosResponse DTO estandarizado con la lista de productos.
     */
    ConsultaProductosResponse obtenerProductos(ConsultaProductosRequest request);

    /**
     * Fuerza la sincronización del catálogo desde el servicio externo de GestoPago.
     * Invocado automáticamente a las 6:00 AM por el programador Cron o de forma manual.
     * 
     * @return ConsultaProductosResponse Resultado de la sincronización.
     */
    ConsultaProductosResponse sincronizarProductos();
}
