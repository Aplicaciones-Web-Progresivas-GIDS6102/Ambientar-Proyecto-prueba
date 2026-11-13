package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.Impl.GestoPagoTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoTokenServiceImplTest {

    @Mock
    private GestoPagoAuthClient authClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private GestoPagoTokenMapper tokenMapper;

    @InjectMocks
    private GestoPagoTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "idDistribuidor", 1001);
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", "DEV01");
        ReflectionTestUtils.setField(tokenService, "password", "secret");
    }

    @Test
    @DisplayName("Renovar token cuando no existe registro previo en BD")
    void renovarToken_CreaNuevoRegistro() {
        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setToken("nuevo_token_jwt");

        when(authClient.authenticate(1001, "DEV01", "secret")).thenReturn(authResp);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(1001, "DEV01")).thenReturn(Optional.empty());
        when(tokenMapper.toEntity(authResp)).thenReturn(new GestoPagoToken());

        tokenService.renovarToken();

        verify(tokenRepository).save(any(GestoPagoToken.class));
    }

    @Test
    @DisplayName("Renovar token cuando ya existe registro previo en BD")
    void renovarToken_ActualizaExistente() {
        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setToken("token_actualizado");
        GestoPagoToken tokenExistente = new GestoPagoToken();

        when(authClient.authenticate(1001, "DEV01", "secret")).thenReturn(authResp);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(1001, "DEV01")).thenReturn(Optional.of(tokenExistente));

        tokenService.renovarToken();

        verify(tokenMapper).updateEntity(authResp, tokenExistente);
        verify(tokenRepository).save(tokenExistente);
    }

    @Test
    @DisplayName("Renovar token con respuesta nula no falla")
    void renovarToken_RespuestaNula() {
        when(authClient.authenticate(1001, "DEV01", "secret")).thenReturn(null);

        tokenService.renovarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Obtener token activo por distribuidor y dispositivo")
    void obtenerTokenActivo_Exitoso() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token123");
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(1001, "DEV01")).thenReturn(Optional.of(token));

        Optional<GestoPagoToken> resultado = tokenService.obtenerTokenActivo(1001, "DEV01");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getToken()).isEqualTo("token123");
    }
}
