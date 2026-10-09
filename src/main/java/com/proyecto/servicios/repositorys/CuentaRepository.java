package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Cuenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Cuenta.
 */
@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    @EntityGraph(attributePaths = {"cliente", "saldo", "estatusCuenta"})
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    boolean existsByNumeroCuenta(String numeroCuenta);

    @EntityGraph(attributePaths = {"cliente", "saldo", "estatusCuenta"})
    List<Cuenta> findByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "saldo", "estatusCuenta"})
    List<Cuenta> findByEstatusCuentaNombre(String nombreEstatus);

    @EntityGraph(attributePaths = {"cliente", "saldo", "estatusCuenta"})
    List<Cuenta> findByEstatusCuentaActivoTrue();
}
