package com.proyecto.servicios.mapper;

import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.entity.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClienteMapperTest {

    @Test
    @DisplayName("Sobrecarga toDTO(Cliente): Mapea entidad individual a Response DTO")
    void toDTO_EntidadIndividual_Exitoso() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("ANA")
                .apellidoPaterno("LOPEZ")
                .correo("ana@example.com")
                .activo(true)
                .build();

        ClienteResponseDTO dto = ClienteMapper.toDTO(cliente);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("ANA", dto.getNombre());
        assertEquals("ana@example.com", dto.getCorreo());
    }

    @Test
    @DisplayName("Sobrecarga toDTO(List<Cliente>): Mapea lista de entidades a lista de DTOs")
    void toDTO_ListaEntidades_Exitoso() {
        Cliente cliente1 = Cliente.builder().id(1L).nombre("ANA").correo("ana@example.com").activo(true).build();
        Cliente cliente2 = Cliente.builder().id(2L).nombre("LUIS").correo("luis@example.com").activo(true).build();

        List<ClienteResponseDTO> dtos = ClienteMapper.toDTO(List.of(cliente1, cliente2));

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("ANA", dtos.get(0).getNombre());
        assertEquals("LUIS", dtos.get(1).getNombre());
    }

    @Test
    @DisplayName("Sobrecarga toDTO(List<Cliente>): Lista nula o vacía devuelve lista vacía")
    void toDTO_ListaNula_DevuelveListaVacia() {
        assertTrue(ClienteMapper.toDTO((List<Cliente>) null).isEmpty());
        assertTrue(ClienteMapper.toDTO(Collections.emptyList()).isEmpty());
    }
}
