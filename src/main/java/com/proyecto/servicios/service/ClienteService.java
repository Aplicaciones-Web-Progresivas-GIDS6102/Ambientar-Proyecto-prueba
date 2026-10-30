package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.ClienteBajaRequestDTO;
import com.proyecto.servicios.dto.ClienteFiltroRequestDTO;
import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;

import java.util.List;

/**
 * Interfaz de servicio para la gestión de Clientes (Usuarios).
 * Expone exclusivamente las operaciones del alcance permitido:
 * 1. Registro de usuarios.
 * 2. Consulta mediante filtros combinables (con sobrecarga por nombre y correo).
 * 3. Borrado lógico de usuarios.
 */
public interface ClienteService {

    /**
     * Registra un nuevo cliente con sus datos validados y genera automáticamente su cuenta inicial.
     *
     * @param dto Datos del nuevo cliente (Request DTO)
     * @return Cliente registrado (Response DTO)
     */
    ClienteResponseDTO crearCliente(ClienteRequestDTO dto);

    /**
     * Consulta clientes activos combinando los criterios definidos en el filtro.
     * Excluye a los usuarios con baja lógica.
     *
     * @param filtro Criterios combinables (Request DTO)
     * @return Lista de clientes activos encontrados
     */
    List<ClienteResponseDTO> buscarClientes(ClienteFiltroRequestDTO filtro);

    /**
     * Sobrecarga de búsqueda: Consulta clientes activos filtrando por nombre y correo.
     * Reutiliza la consulta dinámica sin duplicar lógica.
     *
     * @param nombre Filtro parcial de nombre
     * @param correo Filtro exacto de correo
     * @return Lista de clientes activos encontrados
     */
    List<ClienteResponseDTO> buscarClientes(String nombre, String correo);

    /**
     * Ejecuta el borrado lógico de un cliente activo y desactiva sus cuentas asociadas,
     * conservando el registro físico en la base de datos.
     *
     * @param request Solicitud con el identificador del cliente (Request DTO)
     * @return Cliente actualizado con estado de baja lógica (Response DTO)
     */
    ClienteResponseDTO darDeBaja(ClienteBajaRequestDTO request);
}
