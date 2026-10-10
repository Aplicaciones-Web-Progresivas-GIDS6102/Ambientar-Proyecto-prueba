package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Cliente;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"nacionalidad", "estadoCivil", "domicilio", "domicilio.pais", "informacionLaboral", "cuentas", "cuentas.saldo", "cuentas.estatusCuenta"})
    Optional<Cliente> findById(Long id);

    @EntityGraph(attributePaths = {"nacionalidad", "estadoCivil", "domicilio", "domicilio.pais", "informacionLaboral", "cuentas", "cuentas.saldo", "cuentas.estatusCuenta"})
    Optional<Cliente> findByCurp(String curp);
    boolean existsByCurp(String curp);

    @EntityGraph(attributePaths = {"nacionalidad", "estadoCivil", "domicilio", "domicilio.pais", "informacionLaboral", "cuentas", "cuentas.saldo", "cuentas.estatusCuenta"})
    Optional<Cliente> findByRfc(String rfc);
    boolean existsByRfc(String rfc);

    @EntityGraph(attributePaths = {"nacionalidad", "estadoCivil", "domicilio", "domicilio.pais", "informacionLaboral", "cuentas", "cuentas.saldo", "cuentas.estatusCuenta"})
    Optional<Cliente> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    @EntityGraph(attributePaths = {"nacionalidad", "estadoCivil", "domicilio", "domicilio.pais", "informacionLaboral", "cuentas", "cuentas.saldo", "cuentas.estatusCuenta"})
    List<Cliente> findByActivoTrue();

    List<Cliente> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    @EntityGraph(attributePaths = {"nacionalidad", "estadoCivil", "domicilio", "domicilio.pais", "informacionLaboral", "cuentas", "cuentas.saldo", "cuentas.estatusCuenta"})
    List<Cliente> findByActivoTrueAndFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);
}
