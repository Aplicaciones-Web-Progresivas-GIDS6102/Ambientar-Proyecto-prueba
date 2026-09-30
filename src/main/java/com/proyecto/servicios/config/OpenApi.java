package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración centralizada de SpringDoc OpenAPI / Swagger UI.
 * 
 * Justificación Técnica: Se añade la anotación @Configuration para que Spring gestione el Bean de OpenAPI
 * y se parametriza la URL base con el puerto real del servidor (8090) definido en application.properties.
 */
@Configuration
public class OpenApi {

    @Value("${server.port:8090}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Backend Servicios - Proyecto Integrador")
                        .version("1.0.0")
                        .description("Servicios REST para integración de servicios y plataforma base"))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Servidor Local Activo")
                ));
    }
}
