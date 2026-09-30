package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.BiometriaCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad BiometriaCliente.
 */
@Repository
public interface BiometriaClienteRepository extends JpaRepository<BiometriaCliente, Long> {

    List<BiometriaCliente> findByClienteId(Long clienteId);
}
