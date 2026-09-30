package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Cuenta;
import com.proyecto.servicios.enums.EstadoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Cuenta.
 */
@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    boolean existsByNumeroCuenta(String numeroCuenta);

    Optional<Cuenta> findByClabe(String clabe);
    boolean existsByClabe(String clabe);

    List<Cuenta> findByClienteId(Long clienteId);

    List<Cuenta> findByActivoTrue();

    List<Cuenta> findByEstado(EstadoCuenta estado);
}
