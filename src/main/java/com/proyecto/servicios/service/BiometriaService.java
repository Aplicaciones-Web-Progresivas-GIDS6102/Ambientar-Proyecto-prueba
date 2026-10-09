package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.BiometriaClienteDTO;

import java.util.List;

public interface BiometriaService {

    BiometriaClienteDTO registrarBiometria(BiometriaClienteDTO dto);

    List<BiometriaClienteDTO> obtenerBiometriasPorCliente(Long clienteId);

    BiometriaClienteDTO desactivarBiometria(Long id);
}
