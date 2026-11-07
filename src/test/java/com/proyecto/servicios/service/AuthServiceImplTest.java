package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.AuthResponseDTO;
import com.proyecto.servicios.dto.LoginRequestDTO;
import com.proyecto.servicios.dto.RegisterRequestDTO;
import com.proyecto.servicios.dto.UsuarioResponseDTO;
import com.proyecto.servicios.entity.Cliente;
import com.proyecto.servicios.entity.Sesion;
import com.proyecto.servicios.entity.Usuario;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioYaExisteException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.SesionRepository;
import com.proyecto.servicios.repositorys.UsuarioRepository;
import com.proyecto.servicios.security.JwtUtil;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private SesionRepository sesionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private Cliente clienteMock;
    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        clienteMock = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Perez")
                .activo(true)
                .build();

        usuarioMock = Usuario.builder()
                .id(10L)
                .cliente(clienteMock)
                .username("juanperez")
                .passwordHash("encodedPassword123")
                .activo(true)
                .bloqueado(false)
                .intentosFallidos(0)
                .build();
    }

    @Test
    @DisplayName("Login Exitoso: Retorna token JWT y registra sesión activa")
    void login_Exitoso() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("juanperez")
                .password("password123")
                .build();

        when(usuarioRepository.findByUsername("juanperez")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(true);
        when(jwtUtil.generateToken("juanperez", 1L)).thenReturn("mock.jwt.token");
        when(sesionRepository.findByUsuarioId(10L)).thenReturn(new ArrayList<>());

        AuthResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("juanperez", response.getUsername());
        assertEquals(1L, response.getClienteId());

        verify(sesionRepository, times(1)).save(any(Sesion.class));
    }

    @Test
    @DisplayName("Login Falla: Contraseña incorrecta incrementa intentos fallidos")
    void login_Falla_ContrasenaIncorrecta() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("juanperez")
                .password("wrongpassword")
                .build();

        when(usuarioRepository.findByUsername("juanperez")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword123")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));

        assertEquals(1, usuarioMock.getIntentosFallidos());
        verify(usuarioRepository, times(1)).save(usuarioMock);
    }

    @Test
    @DisplayName("Login Falla: Usuario o contraseña no existen")
    void login_Falla_UsuarioNoExiste() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("inexistente")
                .password("password123")
                .build();

        when(usuarioRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login Falla: Usuario bloqueado")
    void login_Falla_UsuarioBloqueado() {
        usuarioMock.setBloqueado(true);
        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("juanperez")
                .password("password123")
                .build();

        when(usuarioRepository.findByUsername("juanperez")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(true);

        assertThrows(ValidacionNegocioException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Registro Exitoso: Crea nuevo usuario para cliente existente")
    void register_Exitoso() {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .clienteId(1L)
                .username("nuevousuario")
                .password("pass123")
                .build();

        when(usuarioRepository.existsByUsername("nuevousuario")).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteMock));
        when(usuarioRepository.findByClienteId(1L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(20L);
            u.setFechaCreacion(LocalDateTime.now());
            return u;
        });

        UsuarioResponseDTO response = authService.register(request);

        assertNotNull(response);
        assertEquals(20L, response.getId());
        assertEquals("nuevousuario", response.getUsername());
        assertEquals(1L, response.getClienteId());
    }

    @Test
    @DisplayName("Registro Falla: Nombre de usuario ya existe")
    void register_Falla_UsernameExiste() {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .clienteId(1L)
                .username("juanperez")
                .password("pass123")
                .build();

        when(usuarioRepository.existsByUsername("juanperez")).thenReturn(true);

        assertThrows(UsuarioYaExisteException.class, () -> authService.register(request));
    }
}
