package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Domicilio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Domicilio.
 */
@Repository
public interface DomicilioRepository extends JpaRepository<Domicilio, Long> {

    Optional<Domicilio> findByClienteId(Long clienteId);
}
