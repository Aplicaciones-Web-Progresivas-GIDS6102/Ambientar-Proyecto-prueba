package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.dto.BiometriaClienteDTO;
import com.proyecto.servicios.entity.BiometriaCliente;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.BiometriaClienteRepository;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.service.BiometriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BiometriaServiceImpl implements BiometriaService {

    private final BiometriaClienteRepository biometriaRepository;
    private final ClienteRepository clienteRepository;

    @Override
    @Transactional
    public BiometriaClienteDTO registrarBiometria(BiometriaClienteDTO dto) {
        log.info("Registrando muestra biométrica tipo {} para el cliente ID={}", dto.getTipoBiometria(), dto.getClienteId());

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + dto.getClienteId()));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("No se puede registrar biometría para un cliente inactivo o dado de baja.");
        }

        BiometriaCliente biometria = BiometriaCliente.builder()
                .cliente(cliente)
                .tipoBiometria(dto.getTipoBiometria())
                .hashBiometrico(dto.getHashBiometrico())
                .algoritmo(dto.getAlgoritmo() != null ? dto.getAlgoritmo() : "SHA-256")
                .puntuacionCalidad(dto.getPuntuacionCalidad())
                .activo(true)
                .build();

        BiometriaCliente guardada = biometriaRepository.save(biometria);

        return toDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiometriaClienteDTO> obtenerBiometriasPorCliente(Long clienteId) {
        return biometriaRepository.findByClienteId(clienteId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BiometriaClienteDTO desactivarBiometria(Long id) {
        BiometriaCliente biometria = biometriaRepository.findById(id)
                .orElseThrow(() -> new ValidacionNegocioException("No se encontró el registro biométrico con ID: " + id));

        biometria.setActivo(false);
        BiometriaCliente actualizada = biometriaRepository.save(biometria);
        return toDTO(actualizada);
    }

    private BiometriaClienteDTO toDTO(BiometriaCliente b) {
        return BiometriaClienteDTO.builder()
                .id(b.getId())
                .clienteId(b.getCliente().getId())
                .tipoBiometria(b.getTipoBiometria())
                .hashBiometrico(b.getHashBiometrico())
                .algoritmo(b.getAlgoritmo())
                .puntuacionCalidad(b.getPuntuacionCalidad())
                .activo(b.getActivo())
                .fechaRegistro(b.getFechaRegistro())
                .build();
    }
}
