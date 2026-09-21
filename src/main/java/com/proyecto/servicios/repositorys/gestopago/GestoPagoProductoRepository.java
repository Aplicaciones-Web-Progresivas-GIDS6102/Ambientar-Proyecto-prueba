package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


 // Proporciona métodos de acceso a la base de datos PostgreSQL para operaciones CRUD y vaciado masivo.
 //deleteAllInBatch(): Elimina todos los registros de la tabla en un solo query SQL sin cargar entidades en memoria,
 // lo cual se ejecuta ÚNICAMENTE al recibir HTTP 200 OK del proveedor externo para evitar registros duplicados.

@Repository
public interface GestoPagoProductoRepository extends JpaRepository<GestoPagoProducto, Long> {
}
