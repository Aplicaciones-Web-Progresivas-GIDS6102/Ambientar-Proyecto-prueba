package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz de servicio para la gestión completa del Onboarding y ciclo de vida de Clientes.
 */
public interface ClienteService {

    ClienteResponseDTO crearCliente(ClienteRequestDTO dto);

    ClienteResponseDTO obtenerClientePorId(Long id);

    List<ClienteResponseDTO> obtenerClientesActivos();

    ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO dto);

    void eliminarCliente(Long id);

    ClienteResponseDTO buscarPorCurp(String curp);

    ClienteResponseDTO buscarPorRfc(String rfc);

    ClienteResponseDTO buscarPorCorreo(String correo);

    List<ClienteResponseDTO> buscarPorFechaCreacion(LocalDateTime fechaInicio, LocalDateTime fechaFin);
}
