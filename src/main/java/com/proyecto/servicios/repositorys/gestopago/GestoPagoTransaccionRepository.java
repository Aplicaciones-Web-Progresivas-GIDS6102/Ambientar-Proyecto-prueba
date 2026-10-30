package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GestoPagoTransaccionRepository extends JpaRepository<GestoPagoTransaccion, Long> {

    Optional<GestoPagoTransaccion> findByUpc(String upc);

    boolean existsByUpc(String upc);

    boolean existsByCuentaIdAndIdServicioAndIdProductoAndMontoAndEstadoIn(
            Long cuentaId, String idServicio, String idProducto, BigDecimal monto, Collection<String> estados);

    Optional<GestoPagoTransaccion> findFirstByCuentaIdAndIdServicioAndIdProductoAndMontoAndEstadoIn(
            Long cuentaId, String idServicio, String idProducto, BigDecimal monto, Collection<String> estados);

    List<GestoPagoTransaccion> findByEstado(String estado);
}
