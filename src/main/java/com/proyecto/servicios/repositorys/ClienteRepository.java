package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Cliente.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCurp(String curp);
    boolean existsByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);
    boolean existsByRfc(String rfc);

    Optional<Cliente> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Cliente> findByActivoTrueAndFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
}
