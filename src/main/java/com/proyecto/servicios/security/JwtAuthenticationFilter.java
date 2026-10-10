package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.Sesion;
import com.proyecto.servicios.repositorys.SesionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

/**
 * Filtro de Seguridad JWT con control estricto de inactividad de 5 minutos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SesionRepository sesionRepository;

    @Value("${jwt.session-inactivity-minutes:5}")
    private long inactivityTimeoutMinutes;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Rutas públicas de Autenticación, Swagger y GestoPago
        if (path.startsWith("/auth/") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/webjars") ||
            path.startsWith("/gestopago") ||
            path.startsWith("/api/v1/gestopago") ||
            path.startsWith("/api/v1/productos")) {
            return true;
        }

        // Onboarding público de creación de cliente (POST /clientes y POST /api/v1/clientes)
        if ("POST".equalsIgnoreCase(method) &&
            ("/clientes".equals(path) || "/clientes/".equals(path) ||
             "/api/v1/clientes".equals(path) || "/api/v1/clientes/".equals(path))) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = parseBearerToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsernameFromToken(token);

            // Verificar sesión activa e inactividad en la base de datos
            Optional<Sesion> sesionOpt = sesionRepository.findByTokenSesion(token);

            if (sesionOpt.isPresent()) {
                Sesion sesion = sesionOpt.get();

                if (!Boolean.TRUE.equals(sesion.getActiva())) {
                    log.warn("Intento de acceso con sesión inactiva/cerrada para usuario: {}", username);
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "401", "La sesión del usuario ha sido inhabilitada.");
                    return;
                }

                // Control de inactividad de 5 minutos (o configurado)
                LocalDateTime ahora = LocalDateTime.now();
                LocalDateTime limiteInactividad = sesion.getFechaUltimaActividad().plusMinutes(inactivityTimeoutMinutes);

                if (ahora.isAfter(limiteInactividad)) {
                    log.warn("Sesión expirada por inactividad (>{} min) para usuario: {}", inactivityTimeoutMinutes, username);
                    sesion.setActiva(false);
                    sesionRepository.save(sesion);
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "401", "Sesión expirada por inactividad de " + inactivityTimeoutMinutes + " minutos.");
                    return;
                }

                // Actualizar fecha de última actividad (heartbeat de sesión activa)
                sesion.setFechaUltimaActividad(ahora);
                sesionRepository.save(sesion);

                // Autenticar en el SecurityContext de Spring Security
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String parseBearerToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String codigo, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = String.format("{\"codigo\":\"%s\",\"mensaje\":\"%s\",\"estado\":%d,\"timestamp\":\"%s\"}",
                codigo, mensaje, status.value(), LocalDateTime.now());
        response.getWriter().write(body);
    }
}
