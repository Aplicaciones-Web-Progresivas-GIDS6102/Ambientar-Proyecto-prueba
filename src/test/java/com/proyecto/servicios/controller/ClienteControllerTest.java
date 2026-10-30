package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.dto.ClienteBajaRequestDTO;
import com.proyecto.servicios.dto.ClienteFiltroRequestDTO;
import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.dto.ClienteResponseDTO;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/clientes debe responder 201 Created con ClienteResponseDTO")
    void crearCliente_Valido_Retorna201() throws Exception {
        ClienteResponseDTO responseDTO = ClienteResponseDTO.builder()
                .id(1L)
                .nombre("JUAN")
                .correo("juan@example.com")
                .activo(true)
                .build();

        when(clienteService.crearCliente(any(ClienteRequestDTO.class))).thenReturn(responseDTO);

        String jsonValido = """
                {
                  "nombre": "JUAN",
                  "apellidoPaterno": "PEREZ",
                  "apellidoMaterno": "LOPEZ",
                  "fechaNacimiento": "1990-05-15",
                  "curp": "PELJ900515HMCRGR01",
                  "rfc": "PELJ900515XX1",
                  "sexo": "M",
                  "correo": "juan@example.com",
                  "telefonoMovil": "5512345678",
                  "domicilio": {
                    "calle": "Reforma",
                    "numeroExterior": "100",
                    "colonia": "Centro",
                    "municipio": "Cuauhtemoc",
                    "estado": "CDMX",
                    "codigoPostal": "06000"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("JUAN"))
                .andExpect(jsonPath("$.correo").value("juan@example.com"))
                .andExpect(jsonPath("$.activo").value(true));

        verify(clienteService, times(1)).crearCliente(any(ClienteRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/clientes/busquedas debe responder 200 OK con filtros combinables")
    void buscarClientes_ConFiltros_Retorna200() throws Exception {
        ClienteResponseDTO dto = ClienteResponseDTO.builder()
                .id(1L)
                .nombre("JUAN")
                .correo("juan@example.com")
                .activo(true)
                .build();

        when(clienteService.buscarClientes(any(ClienteFiltroRequestDTO.class))).thenReturn(List.of(dto));

        String jsonFiltro = """
                {
                  "nombre": "JUAN",
                  "correo": "juan@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/clientes/busquedas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonFiltro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nombre").value("JUAN"));

        verify(clienteService, times(1)).buscarClientes(any(ClienteFiltroRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/clientes/busquedas con cuerpo vacío debe responder 200 OK")
    void buscarClientes_CuerpoVacio_Retorna200() throws Exception {
        ClienteResponseDTO dto = ClienteResponseDTO.builder().id(1L).nombre("JUAN").activo(true).build();
        when(clienteService.buscarClientes(any())).thenReturn(List.of(dto));

        mockMvc.perform(post("/api/v1/clientes/busquedas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/clientes/baja debe responder 200 OK con ClienteResponseDTO actualizado")
    void darDeBaja_Valido_Retorna200() throws Exception {
        ClienteResponseDTO dto = ClienteResponseDTO.builder()
                .id(10L)
                .nombre("JUAN")
                .activo(false)
                .build();

        when(clienteService.darDeBaja(any(ClienteBajaRequestDTO.class))).thenReturn(dto);

        mockMvc.perform(patch("/api/v1/clientes/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 10, \"motivo\": \"Baja solicitada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.activo").value(false));

        verify(clienteService, times(1)).darDeBaja(any(ClienteBajaRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/clientes/baja con id inválido (negativo o nulo) debe responder 400 Bad Request")
    void darDeBaja_IdInvalido_Retorna400() throws Exception {
        mockMvc.perform(patch("/api/v1/clientes/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("Datos no validos:")))
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("id")));

        verify(clienteService, never()).darDeBaja(any());
    }

    // ==========================================
    // PRUEBAS DE CUMPLIMIENTO ARQUITECTÓNICO
    // ==========================================

    @Test
    @DisplayName("Regla Arquitectónica: Ningún método de ClienteController debe utilizar @PathVariable")
    void verificarAusenciaDePathVariable() {
        Method[] methods = Arrays.stream(ClienteController.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic())
                .toArray(Method[]::new);
        for (Method method : methods) {
            for (Parameter param : method.getParameters()) {
                assertFalse(param.isAnnotationPresent(PathVariable.class),
                        "El método " + method.getName() + " contiene @PathVariable, lo cual está prohibido.");
            }
        }
    }

    @Test
    @DisplayName("Regla Arquitectónica: Ningún método de ClienteController debe utilizar @RequestParam")
    void verificarAusenciaDeRequestParam() {
        Method[] methods = Arrays.stream(ClienteController.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic())
                .toArray(Method[]::new);
        for (Method method : methods) {
            for (Parameter param : method.getParameters()) {
                assertFalse(param.isAnnotationPresent(RequestParam.class),
                        "El método " + method.getName() + " contiene @RequestParam, lo cual está prohibido.");
            }
        }
    }

    @Test
    @DisplayName("Regla Arquitectónica: Todos los métodos de ClienteController deben usar @RequestBody y @Valid")
    void verificarUsoDeRequestBodyYValid() {
        Method[] methods = Arrays.stream(ClienteController.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic())
                .toArray(Method[]::new);
        assertEquals(3, methods.length, "ClienteController debe exponer exactamente 3 métodos para las 3 funcionalidades.");

        for (Method method : methods) {
            boolean tieneRequestBody = false;
            boolean tieneValid = false;

            for (Parameter param : method.getParameters()) {
                if (param.isAnnotationPresent(RequestBody.class)) {
                    tieneRequestBody = true;
                }
                if (param.isAnnotationPresent(Valid.class)) {
                    tieneValid = true;
                }
            }

            assertTrue(tieneRequestBody, "El método " + method.getName() + " debe recibir sus datos mediante @RequestBody.");
            assertTrue(tieneValid, "El método " + method.getName() + " debe activar validaciones mediante @Valid.");
        }
    }

    @Test
    @DisplayName("Regla Arquitectónica: ClienteController nunca debe recibir ni retornar entidades JPA")
    void verificarAislamientoEntidadesJPA() {
        Method[] methods = Arrays.stream(ClienteController.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic())
                .toArray(Method[]::new);
        List<String> paquetesEntidades = List.of("com.proyecto.servicios.entity");

        for (Method method : methods) {
            // Verificar tipo de retorno
            Class<?> returnType = method.getReturnType();
            for (String pkg : paquetesEntidades) {
                assertFalse(returnType.getName().startsWith(pkg),
                        "El método " + method.getName() + " retorna directamente una entidad JPA: " + returnType.getName());
            }

            // Verificar parámetros
            for (Parameter param : method.getParameters()) {
                for (String pkg : paquetesEntidades) {
                    assertFalse(param.getType().getName().startsWith(pkg),
                            "El método " + method.getName() + " recibe una entidad JPA en sus parámetros: " + param.getType().getName());
                }
            }
        }
    }
}
