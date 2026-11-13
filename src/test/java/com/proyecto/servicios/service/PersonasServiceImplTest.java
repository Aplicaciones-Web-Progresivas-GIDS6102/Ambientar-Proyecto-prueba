package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.Impl.PersonasServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonasServiceImplTest {

    @Mock
    private PersonasRepository personasRepository;

    @InjectMocks
    private PersonasServiceImpl personasService;

    @Test
    @DisplayName("Crea persona exitosamente")
    void creaPersona_Exitoso() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Juan");
        request.setApellidoP("Perez");
        request.setApellidoMaterno("Lopez");

        when(personasRepository.save(any(Personas.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonaResponse response = personasService.creaPersona(request);

        assertThat(response).isNotNull();
        assertThat(response.getCodigo()).isEqualTo(1);
        assertThat(response.getMensaje()).isEqualTo("Exito");
    }

    @Test
    @DisplayName("Elimina persona existente")
    void eliminaPersona_Existe() {
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        request.setNombre("Juan");

        Personas persona = new Personas();
        persona.setNombre("Juan");
        when(personasRepository.findByNombre("Juan")).thenReturn(Optional.of(persona));

        GenericResponse response = personasService.eliminaPersona(request);

        assertThat(response.getCodigo()).isEqualTo(0);
        verify(personasRepository).delete(persona);
    }

    @Test
    @DisplayName("Elimina persona no existente")
    void eliminaPersona_NoExiste() {
        EliminaPersonaRequest request = new EliminaPersonaRequest();
        request.setNombre("Desconocido");

        when(personasRepository.findByNombre("Desconocido")).thenReturn(Optional.empty());

        GenericResponse response = personasService.eliminaPersona(request);

        assertThat(response.getCodigo()).isEqualTo(1);
        verify(personasRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Actualiza persona existente")
    void actualizaPersona_Existe() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Juan");
        request.setApellidoP("NuevoP");
        request.setApellidoMaterno("NuevoM");

        Personas persona = new Personas();
        persona.setNombre("Juan");
        when(personasRepository.findByNombre("Juan")).thenReturn(Optional.of(persona));

        GenericResponse response = personasService.actualizaPersona(request);

        assertThat(response.getCodigo()).isEqualTo(0);
        verify(personasRepository).save(persona);
    }

    @Test
    @DisplayName("Actualiza persona no existente")
    void actualizaPersona_NoExiste() {
        PersonasRequest request = new PersonasRequest();
        request.setNombre("Desconocido");

        when(personasRepository.findByNombre("Desconocido")).thenReturn(Optional.empty());

        GenericResponse response = personasService.actualizaPersona(request);

        assertThat(response.getCodigo()).isEqualTo(1);
    }
}
