package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaisRepository extends JpaRepository<Pais, Long> {
    Optional<Pais> findByNombreIgnoreCase(String nombre);
    Optional<Pais> findByCodigoIsoIgnoreCase(String codigoIso);
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByCodigoIsoIgnoreCase(String codigoIso);
}
