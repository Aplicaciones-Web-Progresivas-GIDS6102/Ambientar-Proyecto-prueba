package com.proyecto.servicios.config;

import com.proyecto.servicios.controller.ClienteController;
import com.proyecto.servicios.dto.ClienteBajaRequestDTO;
import com.proyecto.servicios.dto.ClienteFiltroRequestDTO;
import com.proyecto.servicios.dto.ClienteRequestDTO;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private GlobalExceptionHandler exceptionHandler;

    private static final String JSON_CLIENTE_VALIDO = """
            {
              "nombre": "CARLOS",
              "apellidoPaterno": "HERNANDEZ",
              "apellidoMaterno": "GOMEZ",
              "fechaNacimiento": "1990-05-15",
              "curp": "HERC900515HMCRGR01",
              "rfc": "HERC900515XX1",
              "sexo": "M",
              "correo": "test@email.com",
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

    @Mock
    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ClienteController(clienteService))
                .setControllerAdvice(exceptionHandler)
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    // ============================================================
    // 1. PRUEBAS DE INTEGRACIÓN CON MOCKMVC: BEAN VALIDATION (@Valid)
    // ============================================================

    @Test
    @DisplayName("Debe devolver GenericResponse 400 con código 1 cuando faltan campos obligatorios en registro")
    void crearCliente_CamposObligatoriosAusentes_Devuelve400ConGenericResponse() throws Exception {
        String jsonVacio = "{}";

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonVacio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")))
                .andExpect(jsonPath("$.mensaje", containsString("nombre")))
                .andExpect(jsonPath("$.mensaje", containsString("apellidoPaterno")))
                .andExpect(jsonPath("$.mensaje", containsString("apellidoMaterno")))
                .andExpect(jsonPath("$.mensaje", containsString("fechaNacimiento")))
                .andExpect(jsonPath("$.mensaje", containsString("curp")))
                .andExpect(jsonPath("$.mensaje", containsString("rfc")))
                .andExpect(jsonPath("$.mensaje", containsString("sexo")))
                .andExpect(jsonPath("$.mensaje", containsString("correo")))
                .andExpect(jsonPath("$.mensaje", containsString("domicilio")));
    }

    @Test
    @DisplayName("Debe devolver GenericResponse 400 cuando un campo obligatorio único está ausente (ej. correo ausente)")
    void crearCliente_CampoObligatorioAusente_Devuelve400() throws Exception {
        String jsonSinCorreo = """
                {
                  "nombre": "CARLOS",
                  "apellidoPaterno": "HERNANDEZ",
                  "apellidoMaterno": "GOMEZ",
                  "fechaNacimiento": "1990-05-15",
                  "curp": "HERC900515HMCRGR01",
                  "rfc": "HERC900515XX1",
                  "sexo": "M",
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
                        .content(jsonSinCorreo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")))
                .andExpect(jsonPath("$.mensaje", containsString("correo")));
    }

    @Test
    @DisplayName("Debe devolver GenericResponse 400 cuando el correo electrónico tiene un formato inválido")
    void crearCliente_CorreoInvalido_Devuelve400() throws Exception {
        String jsonCorreoInvalido = """
                {
                  "nombre": "CARLOS",
                  "apellidoPaterno": "HERNANDEZ",
                  "apellidoMaterno": "GOMEZ",
                  "fechaNacimiento": "1990-05-15",
                  "curp": "HERC900515HMCRGR01",
                  "rfc": "HERC900515XX1",
                  "sexo": "M",
                  "correo": "correo-no-valido-sin-arroba",
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
                        .content(jsonCorreoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")))
                .andExpect(jsonPath("$.mensaje", containsString("correo")));
    }

    @Test
    @DisplayName("Debe devolver GenericResponse 400 cuando un campo excede o no cumple la longitud permitida")
    void crearCliente_LongitudInvalida_Devuelve400() throws Exception {
        String jsonLongitudInvalida = """
                {
                  "nombre": "A",
                  "apellidoPaterno": "HERNANDEZ",
                  "apellidoMaterno": "GOMEZ",
                  "fechaNacimiento": "1990-05-15",
                  "curp": "HERC900515HMCRGR01",
                  "rfc": "HERC900515XX1",
                  "sexo": "M",
                  "correo": "test@email.com",
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
                        .content(jsonLongitudInvalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")))
                .andExpect(jsonPath("$.mensaje", containsString("nombre")));
    }

    @Test
    @DisplayName("Debe devolver GenericResponse 400 con los nombres de todos los campos que violan restricciones")
    void crearCliente_MultiplesCamposInvalidos_Devuelve400() throws Exception {
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
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")))
                .andExpect(jsonPath("$.mensaje", containsString("nombre")))
                .andExpect(jsonPath("$.mensaje", containsString("curp")))
                .andExpect(jsonPath("$.mensaje", containsString("rfc")))
                .andExpect(jsonPath("$.mensaje", containsString("correo")))
                .andExpect(jsonPath("$.mensaje", containsString("sexo")))
                .andExpect(jsonPath("$.mensaje", containsString("telefonoMovil")))
                .andExpect(jsonPath("$.mensaje", containsString("telefonoAlternativo")));
    }

    @Test
    @DisplayName("Debe devolver GenericResponse 400 ante criterios de búsqueda inválidos (rango de fechas invertido)")
    void buscarClientes_CriteriosBusquedaInvalidos_Devuelve400() throws Exception {
        String jsonFiltroInvalido = """
                {
                  "fechaCreacionDesde": "2026-12-31T23:59:59",
                  "fechaCreacionHasta": "2026-01-01T00:00:00"
                }
                """;

        mockMvc.perform(post("/api/v1/clientes/busquedas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonFiltroInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")))
                .andExpect(jsonPath("$.mensaje", containsString("rangoFechasValido")));
    }

    @Test
    @DisplayName("Debe devolver GenericResponse 400 ante CURP con formato inválido en los filtros de búsqueda")
    void buscarClientes_CurpInvalidaEnFiltro_Devuelve400() throws Exception {
        String jsonFiltroCurpInvalida = """
                {
                  "curp": "CURPINCORRECTA123"
                }
                """;

        mockMvc.perform(post("/api/v1/clientes/busquedas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonFiltroCurpInvalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")))
                .andExpect(jsonPath("$.mensaje", containsString("curp")));
    }

    @Test
    @DisplayName("Debe devolver GenericResponse 400 ante solicitud de baja lógica con ID inválido")
    void darDeBaja_IdInvalido_Devuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/clientes/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": -10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.mensaje", startsWith("Datos no validos:")))
                .andExpect(jsonPath("$.mensaje", containsString("id")));
    }

    @Test
    @DisplayName("Debe manejar HttpMessageNotReadableException cuando el JSON está malformado devolviendo HTTP 400")
    void handleHttpMessageNotReadable_JsonMalformado_Retorna400() throws Exception {
        String jsonMalformado = "{\"nombre\": \"JUAN\", \"apellidoPaterno\": ";

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMalformado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje", containsString("JSON")));
    }

    // ============================================================
    // 2. PRUEBAS DE ERRORES DE NEGOCIO (400, 404, 409)
    // ============================================================

    @Test
    @DisplayName("Debe manejar ValidacionNegocioException devolviendo GenericResponse con HTTP 400")
    void handleValidacionNegocio_Retorna400() throws Exception {
        when(clienteService.crearCliente(any(ClienteRequestDTO.class)))
                .thenThrow(new ValidacionNegocioException("El cliente debe ser mayor de edad (mínimo 18 años)."));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_CLIENTE_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("El cliente debe ser mayor de edad (mínimo 18 años)."));
    }

    @Test
    @DisplayName("Debe manejar ClienteNoEncontradoException retornando GenericResponse con HTTP 404")
    void handleClienteNoEncontrado_Retorna404() throws Exception {
        when(clienteService.darDeBaja(any(ClienteBajaRequestDTO.class)))
                .thenThrow(new ClienteNoEncontradoException("No se encontró el cliente con ID: 999"));

        mockMvc.perform(patch("/api/v1/clientes/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404))
                .andExpect(jsonPath("$.mensaje").value("No se encontró el cliente con ID: 999"));
    }

    @Test
    @DisplayName("Debe manejar ClienteYaExisteException retornando GenericResponse con HTTP 409")
    void handleClienteYaExiste_Retorna409() throws Exception {
        when(clienteService.crearCliente(any(ClienteRequestDTO.class)))
                .thenThrow(new ClienteYaExisteException("El correo electrónico 'test@email.com' ya está registrado."));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_CLIENTE_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo electrónico 'test@email.com' ya está registrado."));
    }

    @Test
    @DisplayName("Debe manejar CurpDuplicadaException retornando GenericResponse con HTTP 409")
    void handleCurpDuplicada_Retorna409() throws Exception {
        when(clienteService.crearCliente(any(ClienteRequestDTO.class)))
                .thenThrow(new CurpDuplicadaException("La CURP ya se encuentra registrada."));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_CLIENTE_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value("La CURP ya se encuentra registrada."));
    }

    @Test
    @DisplayName("Debe manejar ClienteYaEliminadoException retornando GenericResponse con HTTP 409")
    void handleClienteYaEliminado_Retorna409() throws Exception {
        when(clienteService.darDeBaja(any(ClienteBajaRequestDTO.class)))
                .thenThrow(new ClienteYaEliminadoException("El cliente con ID: 10 ya se encuentra dado de baja."));

        mockMvc.perform(patch("/api/v1/clientes/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value("El cliente con ID: 10 ya se encuentra dado de baja."));
    }

    // ============================================================
    // 3. PRUEBAS DE INTEGRIDAD Y PERSISTENCIA (404, 409)
    // ============================================================

    @Test
    @DisplayName("Debe manejar DataIntegrityViolationException retornando HTTP 409 sin exponer detalles SQL de la BD")
    void handleDataIntegrityViolation_Retorna409Seguro() throws Exception {
        when(clienteService.darDeBaja(any(ClienteBajaRequestDTO.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation: table clientes column curp"));

        mockMvc.perform(patch("/api/v1/clientes/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value("Conflicto de integridad: ya existe un registro con información duplicada o incompatible en el sistema."));
    }

    @Test
    @DisplayName("Debe manejar EntityNotFoundException de JPA retornando GenericResponse con HTTP 404")
    void handleEntityNotFound_Retorna404() throws Exception {
        when(clienteService.darDeBaja(any(ClienteBajaRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Entity not found in persistence context"));

        mockMvc.perform(patch("/api/v1/clientes/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 10}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404))
                .andExpect(jsonPath("$.mensaje").value("El recurso solicitado no fue encontrado."));
    }

    // ============================================================
    // 4. PRUEBAS DE ERRORES INESPERADOS (500)
    // ============================================================

    @Test
    @DisplayName("Debe manejar excepciones inesperadas retornando HTTP 500 sin exponer trazas técnicas")
    void handleGenericException_Retorna500Seguro() throws Exception {
        when(clienteService.darDeBaja(any(ClienteBajaRequestDTO.class)))
                .thenThrow(new NullPointerException("Null reference internally"));

        mockMvc.perform(patch("/api/v1/clientes/baja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 10}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value(500))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado al procesar la solicitud."));
    }

    // ============================================================
    // 5. PRUEBAS UNITARIAS DIRECTAS DE MÉTODOS DEL HANDLER
    // ============================================================

    @Test
    @DisplayName("Prueba Unitaria: dtoValidation produce GenericResponse con código 1 y formato 'Datos no validos: '")
    void testUnitario_dtoValidation() throws NoSuchMethodException {
        ClienteRequestDTO target = new ClienteRequestDTO();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "clienteRequestDTO");
        bindingResult.addError(new FieldError("clienteRequestDTO", "nombre", "El nombre es obligatorio"));
        bindingResult.addError(new FieldError("clienteRequestDTO", "correo", "El formato del correo es inválido"));

        MethodParameter parameter = new MethodParameter(
                ClienteController.class.getMethod("crearCliente", ClienteRequestDTO.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<GenericResponse> response = exceptionHandler.dtoValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().startsWith("Datos no validos:"));
        assertTrue(response.getBody().getMensaje().contains("nombre"));
        assertTrue(response.getBody().getMensaje().contains("correo"));
    }

    @Test
    @DisplayName("Prueba Unitaria: dtoValidation agrupa errores sin pérdida cuando un campo tiene múltiples violaciones")
    void testUnitario_dtoValidation_camposRepetidos() throws NoSuchMethodException {
        ClienteRequestDTO target = new ClienteRequestDTO();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "clienteRequestDTO");
        bindingResult.addError(new FieldError("clienteRequestDTO", "nombre", "El nombre es obligatorio"));
        bindingResult.addError(new FieldError("clienteRequestDTO", "nombre", "El nombre debe tener al menos 2 caracteres"));

        MethodParameter parameter = new MethodParameter(
                ClienteController.class.getMethod("crearCliente", ClienteRequestDTO.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<GenericResponse> response = exceptionHandler.dtoValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("El nombre es obligatorio; El nombre debe tener al menos 2 caracteres"));
    }

    @Test
    @DisplayName("Prueba Unitaria: handleValidacionNegocio genera GenericResponse con HTTP 400")
    void testUnitario_handleValidacionNegocio() {
        ValidacionNegocioException ex = new ValidacionNegocioException("Error de negocio de prueba");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleValidacionNegocio(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCodigo());
        assertEquals("Error de negocio de prueba", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Prueba Unitaria: handleClienteNoEncontrado genera GenericResponse con HTTP 404")
    void testUnitario_handleClienteNoEncontrado() {
        ClienteNoEncontradoException ex = new ClienteNoEncontradoException("Cliente 123 no hallado");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleClienteNoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getCodigo());
        assertEquals("Cliente 123 no hallado", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Prueba Unitaria: handleConflictosExistencia genera GenericResponse con HTTP 409")
    void testUnitario_handleConflictosExistencia() {
        ClienteYaEliminadoException ex = new ClienteYaEliminadoException("Cliente dado de baja");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleConflictosExistencia(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getCodigo());
        assertEquals("Cliente dado de baja", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Prueba Unitaria: handleDataIntegrityViolation sanitiza el mensaje sin exponer SQL")
    void testUnitario_handleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("SELECT * FROM passwords WHERE ... syntax error");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getCodigo());
        assertFalse(response.getBody().getMensaje().contains("passwords"));
        assertFalse(response.getBody().getMensaje().contains("SELECT"));
    }

    @Test
    @DisplayName("Prueba Unitaria: handleGenericException oculta detalles de la excepción interna")
    void testUnitario_handleGenericException() {
        RuntimeException ex = new RuntimeException("Detalle interno confidencial: token_123456");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCodigo());
        assertFalse(response.getBody().getMensaje().contains("token_123456"));
        assertEquals("Ocurrió un error inesperado al procesar la solicitud.", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Prueba Unitaria: handleAccesoDenegado genera GenericResponse con HTTP 403")
    void testUnitario_handleAccesoDenegado() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden action");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleAccesoDenegado(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getCodigo());
    }

    @Test
    @DisplayName("Prueba Unitaria: handleConstraintViolation genera GenericResponse con HTTP 400 y formato ValidadorCampos")
    void testUnitario_handleConstraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException("Violación de restricción", Collections.emptySet());
        ResponseEntity<GenericResponse> response = exceptionHandler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().startsWith("Datos no validos:"));
    }
}
