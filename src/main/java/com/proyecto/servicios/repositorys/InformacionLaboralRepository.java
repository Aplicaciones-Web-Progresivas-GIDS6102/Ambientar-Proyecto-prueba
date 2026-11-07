package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.InformacionLaboral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InformacionLaboralRepository extends JpaRepository<InformacionLaboral, Long> {
    Optional<InformacionLaboral> findByClienteId(Long clienteId);
}
