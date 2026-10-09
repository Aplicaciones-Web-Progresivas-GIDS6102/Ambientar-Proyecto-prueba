package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.EstatusCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstatusCuentaRepository extends JpaRepository<EstatusCuenta, Long> {
    Optional<EstatusCuenta> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
}
