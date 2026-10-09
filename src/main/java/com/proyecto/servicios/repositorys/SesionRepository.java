package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Sesion.
 */
@Repository
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByTokenSesion(String tokenSesion);
    List<Sesion> findByUsuarioId(Long usuarioId);
}
