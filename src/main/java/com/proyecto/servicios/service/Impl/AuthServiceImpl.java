package com.proyecto.servicios.service.Impl;

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
import com.proyecto.servicios.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final SesionRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Intento de inicio de sesión para el usuario: {}", request.getUsername());

        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CredencialesInvalidasException("Nombre de usuario o contraseña incorrectos."));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            log.warn("Contraseña incorrecta para el usuario: {}", request.getUsername());
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            if (usuario.getIntentosFallidos() >= 5) {
                usuario.setBloqueado(true);
            }
            usuarioRepository.save(usuario);
            throw new CredencialesInvalidasException("Nombre de usuario o contraseña incorrectos.");
        }

        if (Boolean.TRUE.equals(usuario.getBloqueado()) || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ValidacionNegocioException("La cuenta de usuario se encuentra bloqueada o inactiva.");
        }

        Cliente cliente = usuario.getCliente();
        if (cliente == null || !Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("El cliente asociado no se encuentra activo.");
        }

        // Restablecer intentos fallidos y actualizar último acceso
        usuario.setIntentosFallidos(0);
        usuario.setFechaUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Generar JWT
        String token = jwtUtil.generateToken(usuario.getUsername(), cliente.getId());

        // Registrar Sesión activa en BD
        Sesion sesion = Sesion.builder()
                .usuario(usuario)
                .tokenSesion(token)
                .activa(true)
                .fechaInicio(LocalDateTime.now())
                .fechaUltimaActividad(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(24))
                .build();
        sesionRepository.save(sesion);

        log.info("Inicio de sesión exitoso para usuario: {} con cliente ID: {}", usuario.getUsername(), cliente.getId());

        return AuthResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .username(usuario.getUsername())
                .clienteId(cliente.getId())
                .expiresInSeconds(86400L)
                .build();
    }

    @Override
    @Transactional
    public UsuarioResponseDTO register(RegisterRequestDTO request) {
        log.info("Registrando usuario para el cliente ID: {}", request.getClienteId());

        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new UsuarioYaExisteException("El nombre de usuario '" + request.getUsername() + "' ya está registrado.");
        }

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + request.getClienteId()));

        if (usuarioRepository.findByClienteId(cliente.getId()).isPresent()) {
            throw new ValidacionNegocioException("El cliente ya posee un usuario registrado.");
        }

        Usuario usuario = Usuario.builder()
                .cliente(cliente)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .intentosFallidos(0)
                .bloqueado(false)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado exitosamente con ID: {}", guardado.getId());

        return UsuarioResponseDTO.builder()
                .id(guardado.getId())
                .clienteId(cliente.getId())
                .username(guardado.getUsername())
                .activo(guardado.getActivo())
                .fechaCreacion(guardado.getFechaCreacion())
                .build();
    }
}
