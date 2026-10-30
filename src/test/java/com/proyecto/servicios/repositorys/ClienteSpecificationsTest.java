package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.dto.ClienteFiltroRequestDTO;
import com.proyecto.servicios.entity.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ClienteSpecificationsTest {

    @Test
    @DisplayName("Debe construir especificación con DTO de filtros completos")
    void construirCriterios_ConFiltroDTO_NoEsNulo() {
        ClienteFiltroRequestDTO filtro = ClienteFiltroRequestDTO.builder()
                .nombre("Carlos")
                .curp("HERC900515HMCRGR01")
                .rfc("HERC900515XX1")
                .correo("carlos@email.com")
                .fechaCreacionDesde(LocalDateTime.now().minusDays(10))
                .fechaCreacionHasta(LocalDateTime.now())
                .build();

        Specification<Cliente> spec = ClienteSpecifications.construirCriterios(filtro);

        assertNotNull(spec, "La especificación generada no debe ser nula.");
    }

    @Test
    @DisplayName("Debe construir especificación con filtro nulo (solo activo=true)")
    void construirCriterios_FiltroNulo_NoEsNulo() {
        Specification<Cliente> spec = ClienteSpecifications.construirCriterios((ClienteFiltroRequestDTO) null);

        assertNotNull(spec, "La especificación sin filtros no debe ser nula.");
    }

    @Test
    @DisplayName("Sobrecarga de métodos: debe construir especificación mediante sobrecarga (nombre, correo)")
    void construirCriterios_SobrecargaNombreCorreo_FuncionaCorrectamente() {
        Specification<Cliente> specSobrecargada = ClienteSpecifications.construirCriterios("Carlos", "carlos@email.com");

        assertNotNull(specSobrecargada, "La especificación sobrecargada no debe ser nula.");
    }

    @Test
    @DisplayName("Método auxiliar normalizar debe recortar cadenas y devolver null si están vacías o en blanco")
    void normalizar_Cadenas_ComportamientoEsperado() {
        assertNull(ClienteSpecifications.normalizar(null));
        assertNull(ClienteSpecifications.normalizar(""));
        assertNull(ClienteSpecifications.normalizar("   "));
        assertEquals("test", ClienteSpecifications.normalizar("  test  "));
    }
}
