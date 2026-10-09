package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.DatosBiometricos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatosBiometricosRepository extends JpaRepository<DatosBiometricos, Long> {
    List<DatosBiometricos> findByClienteId(Long clienteId);
}
