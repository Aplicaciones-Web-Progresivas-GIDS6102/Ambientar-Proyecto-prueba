package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Cliente;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Cliente con soporte de consultas dinámicas por especificación.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    @Override
    @EntityGraph(attributePaths = {"nacionalidad", "estadoCivil", "domicilio", "domicilio.pais", "informacionLaboral", "cuentas", "cuentas.saldo", "cuentas.estatusCuenta"})
    Optional<Cliente> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"nacionalidad", "estadoCivil", "domicilio", "domicilio.pais", "informacionLaboral", "cuentas", "cuentas.saldo", "cuentas.estatusCuenta"})
    List<Cliente> findAll(Specification<Cliente> spec);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    @EntityGraph(attributePaths = {"nacionalidad", "estadoCivil", "domicilio", "domicilio.pais", "informacionLaboral", "cuentas", "cuentas.saldo", "cuentas.estatusCuenta"})
    List<Cliente> findByActivoTrue();
}
