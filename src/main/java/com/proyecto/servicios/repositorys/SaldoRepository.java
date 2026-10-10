package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Saldo.
 */
@Repository
public interface SaldoRepository extends JpaRepository<Saldo, Long> {

    Optional<Saldo> findByCuentaId(Long cuentaId);
}
