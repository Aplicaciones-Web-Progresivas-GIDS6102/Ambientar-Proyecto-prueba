package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByClienteId(Long clienteId);
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
}
