package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.BiometriaClienteDTO;
import com.proyecto.servicios.entity.BiometriaCliente;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.enums.TipoBiometria;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.BiometriaClienteRepository;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.service.Impl.BiometriaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiometriaServiceImplTest {

    @Mock
    private BiometriaClienteRepository biometriaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private BiometriaServiceImpl biometriaService;

    private Cliente clienteActivo;
    private BiometriaCliente biometriaMock;
    private BiometriaClienteDTO dtoMock;

    @BeforeEach
    void setUp() {
        clienteActivo = Cliente.builder().id(1L).activo(true).build();
        biometriaMock = BiometriaCliente.builder()
                .id(10L)
                .cliente(clienteActivo)
                .tipoBiometria(TipoBiometria.HUELLA_DACTILAR)
                .hashBiometrico("hash123")
                .algoritmo("SHA-256")
                .puntuacionCalidad(95)
                .activo(true)
                .build();
        dtoMock = BiometriaClienteDTO.builder()
                .clienteId(1L)
                .tipoBiometria(TipoBiometria.HUELLA_DACTILAR)
                .hashBiometrico("hash123")
                .puntuacionCalidad(95)
                .build();
    }

    @Test
    @DisplayName("Registrar biometría exitosamente")
    void registrarBiometria_Exitoso() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(biometriaRepository.save(any(BiometriaCliente.class))).thenReturn(biometriaMock);

        BiometriaClienteDTO resultado = biometriaService.registrarBiometria(dtoMock);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getClienteId()).isEqualTo(1L);
        assertThat(resultado.getHashBiometrico()).isEqualTo("hash123");
    }

    @Test
    @DisplayName("Lanza excepción si cliente no existe al registrar")
    void registrarBiometria_ClienteNoExiste() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> biometriaService.registrarBiometria(dtoMock));
    }

    @Test
    @DisplayName("Lanza excepción si cliente está inactivo")
    void registrarBiometria_ClienteInactivo() {
        clienteActivo.setActivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));

        assertThrows(ValidacionNegocioException.class, () -> biometriaService.registrarBiometria(dtoMock));
    }

    @Test
    @DisplayName("Obtener biometrías por cliente")
    void obtenerBiometriasPorCliente_Exitoso() {
        when(biometriaRepository.findByClienteId(1L)).thenReturn(List.of(biometriaMock));

        List<BiometriaClienteDTO> lista = biometriaService.obtenerBiometriasPorCliente(1L);

        assertThat(lista).hasSize(1);
    }

    @Test
    @DisplayName("Desactivar biometría existente")
    void desactivarBiometria_Exitoso() {
        when(biometriaRepository.findById(10L)).thenReturn(Optional.of(biometriaMock));
        when(biometriaRepository.save(any(BiometriaCliente.class))).thenReturn(biometriaMock);

        BiometriaClienteDTO resultado = biometriaService.desactivarBiometria(10L);

        assertThat(resultado).isNotNull();
        verify(biometriaRepository).save(biometriaMock);
    }

    @Test
    @DisplayName("Desactivar biometría lanza excepción si no existe")
    void desactivarBiometria_NoExiste() {
        when(biometriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ValidacionNegocioException.class, () -> biometriaService.desactivarBiometria(99L));
    }
}
