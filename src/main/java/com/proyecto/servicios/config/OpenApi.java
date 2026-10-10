package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración centralizada de SpringDoc OpenAPI / Swagger UI con soporte para autenticación Bearer JWT.
 */
@Configuration
public class OpenApi {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("API Backend Servicios Bancarios - Onboarding & GestoPago")
                        .version("1.0.0")
                        .description("Servicios REST completos para onboarding de clientes personas físicas, cuentas bancarias, saldos, biometría, autenticación JWT y GestoPago."))
                .servers(List.of(
                        new Server().url("/").description("Servidor Activo (Local / Railway)"),
                        new Server().url("http://localhost:" + serverPort).description("Servidor Local")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Introduzca el token JWT obtenido del endpoint /auth/login para autorizar las peticiones protegidas.")));
    }
}
