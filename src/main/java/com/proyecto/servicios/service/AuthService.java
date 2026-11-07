package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.AuthResponseDTO;
import com.proyecto.servicios.dto.LoginRequestDTO;
import com.proyecto.servicios.dto.RegisterRequestDTO;
import com.proyecto.servicios.dto.UsuarioResponseDTO;

public interface AuthService {

    AuthResponseDTO login(LoginRequestDTO request);

    UsuarioResponseDTO register(RegisterRequestDTO request);
}
