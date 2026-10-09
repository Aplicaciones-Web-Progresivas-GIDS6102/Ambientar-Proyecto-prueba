package com.proyecto.servicios.config;

import com.proyecto.servicios.controller.ClienteController;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock
    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ClienteController(clienteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("Debe devolver respuesta 400 con lista completa de detalles cuando se envían múltiples parámetros inválidos")
    void crearCliente_MultiplesParametrosInvalidos_DevuelveDetallesList() throws Exception {
        String jsonPayload = """
                {
                  "nombre": "a",
                  "apellidoPaterno": "b",
                  "apellidoMaterno": "c",
                  "curp": "INVALID_CURP",
                  "rfc": "INVALID_RFC",
                  "sexo": "",
                  "correo": "correo_invalido",
                  "telefonoMovil": "llllllllll",
                  "telefonoAlternativo": "4681072996abc",
                  "domicilio": {
                    "calle": "Calle 1",
                    "numeroExterior": "123",
                    "colonia": "Centro",
                    "municipio": "Cuauhtémoc",
                    "estado": "CDMX",
                    "codigoPostal": "06000"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("400"))
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("Error de validación en los parámetros de entrada."))
                .andExpect(jsonPath("$.detalles").isArray())
                .andExpect(jsonPath("$.detalles", hasItem(containsString("telefonoMovil"))))
                .andExpect(jsonPath("$.detalles", hasItem(containsString("telefonoAlternativo"))))
                .andExpect(jsonPath("$.detalles", hasItem(containsString("sexo"))))
                .andExpect(jsonPath("$.detalles", hasItem(containsString("nombre"))))
                .andExpect(jsonPath("$.detalles", hasItem(containsString("curp"))))
                .andExpect(jsonPath("$.detalles", hasItem(containsString("correo"))))
                .andExpect(jsonPath("$.detalles", hasItem(containsString("rfc"))));
    }

    @Test
    @DisplayName("Debe devolver múltiples detalles incluyendo fechaNacimiento inválida sin abortar la validación de otros campos")
    void crearCliente_FechaInvalidaYCamposInvalidos_DevuelveTodosLosDetallesSimultaneamente() throws Exception {
        String jsonPayload = """
                {
                  "nombre": "$%##",
                  "segundoNombre": "",
                  "apellidoPaterno": "#",
                  "apellidoMaterno": "#",
                  "fechaNacimiento": "1990-01-99",
                  "curp": "GODE900115MDFRRL09",
                  "rfc": "GODE9001151A2",
                  "sexo": "F",
                  "correo": "maria.garcia.demo@example.com",
                  "telefonoMovil": "5512345678",
                  "domicilio": {
                    "calle": "Avenida Insurgentes",
                    "numeroExterior": "123",
                    "colonia": "Del Valle",
                    "municipio": "Benito Juarez",
                    "estado": "Ciudad de Mexico",
                    "codigoPostal": "03100"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("400"))
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.detalles").isArray())
                .andExpect(jsonPath("$.detalles", hasItem(containsString("fechaNacimiento"))))
                .andExpect(jsonPath("$.detalles", hasItem(containsString("nombre"))))
                .andExpect(jsonPath("$.detalles", hasItem(containsString("apellidoPaterno"))))
                .andExpect(jsonPath("$.detalles", hasItem(containsString("apellidoMaterno"))));
    }
}
