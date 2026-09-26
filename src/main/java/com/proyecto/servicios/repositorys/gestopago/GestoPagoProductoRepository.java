package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para GestoPagoProducto.
 */
@Repository
public interface GestoPagoProductoRepository extends JpaRepository<GestoPagoProducto, Long> {
}
