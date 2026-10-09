package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.EstadoCivil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoCivilRepository extends JpaRepository<EstadoCivil, Long> {
    Optional<EstadoCivil> findByDescripcionIgnoreCase(String descripcion);
    boolean existsByDescripcionIgnoreCase(String descripcion);
}
