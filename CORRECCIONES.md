# CORRECCIONES DEL PROYECTO: ESTANDARIZACIÓN DE VALIDACIONES Y MANEJO GLOBAL DE EXCEPCIONES AL ESTILO VALIDADORCAMPOS

Auditoría técnica, refactorización, estandarización de Bean Validation y manejo centralizado de excepciones en la API REST de Usuarios/Clientes según el patrón `ValidadorCampos` y el modelo `GenericResponse`.

---

## A. Estado anterior

Antes de las modificaciones, el procesamiento de validaciones y excepciones en el repositorio presentaba las siguientes deficiencias:

1. **Tratamiento disperso y no uniforme de errores:**
   - Controladores secundarios como `PersonaController` capturaban manualmente excepciones dentro de bloques `try-catch`, construyendo respuestas con códigos arbitrarios o devolviendo HTTP 200 OK con mensajes de error incrustados.
   - En `GlobalExceptionHandler`, las respuestas de validación se estructuraban mediante un DTO `ErrorResponse` que contenía listas de strings (`detalles`) o mediante lógica legacy que devolvía HTTP 200 con `ConsultaProductosResponse`.
2. **Ausencia del estándar `GenericResponse` en validaciones:**
   - No existía unificación con el formato canónico `GenericResponse` (`codigo` numérico y `mensaje`).
   - La clase `GenericResponse.java` original del proyecto había quedado en desuso o eliminada, impidiendo un contrato homogéneo para clientes externos.
3. **Formato no estandarizado en la captura de campos inválidos:**
   - No se agrupaban los errores en una estructura asociativa `Map<String, String>` que relacionara cada campo inválido con su mensaje de error, dificultando que los clientes del frontend o consumidores de la API asociaran directamente los errores a los campos del formulario.
   - No se empleaba el prefijo estructurado `"Datos no validos: " + listError` con código de error `1`.
4. **Fuga de información técnica en excepciones de base de datos:**
   - El manejador de `DataIntegrityViolationException` exponía detalles internos de Hibernate/PostgreSQL (`ex.getCause().getMessage()`), filtrando nombres de tablas, columnas y sentencias SQL al cliente.
5. **Endpoints y funcionalidades excedentes:**
   - La API exponía endpoints no autorizados con `@PathVariable` y `@RequestParam`, carecía de un endpoint unificado para borrado lógico y no contaba con sobrecargas de métodos ni componentes reutilizables en las operaciones esenciales.

---

## B. Incumplimientos encontrados

| Archivo | Clase o método | Problema detectado | Corrección aplicada |
|---|---|---|---|
| `GlobalExceptionHandler.java` | `handleValidationExceptions` | Retornaba `ErrorResponse` con estructura divergente (HTTP 400 con arreglo `detalles`), sin seguir el estándar `GenericResponse` ni el patrón de `ValidadorCampos`. | Se refactorizó e implementó el método `dtoValidation(MethodArgumentNotValidException)` retornando `ResponseEntity<GenericResponse>` con `HttpStatus.BAD_REQUEST`, `codigo = 1` y mensaje `"Datos no validos: " + listError`. |
| `GlobalExceptionHandler.java` | Manejo de campos repetidos | Al iterar errores, si un campo presentaba múltiples violaciones (ej. `@NotNull` y `@Size`), se corría el riesgo de sobreescribir silenciosamente la información en el mapa. | Se utilizó `listError.merge(campo, mensaje, (m1, m2) -> m1 + "; " + m2)` para concatenar múltiples violaciones sobre el mismo campo sin perder información. |
| `GlobalExceptionHandler.java` | Manejadores de excepciones de negocio | Las excepciones de negocio y no encontradas devolvían DTOs `ErrorResponse` dispares con tipos String en los códigos de error. | Se homogeneizaron todos los manejadores para retornar `ResponseEntity<GenericResponse>` con códigos numéricos consistentes (`400`, `404`, `409`, `500`). |
| `GlobalExceptionHandler.java` | `handleDataIntegrityViolation` | Exponía fragmentos de sentencias SQL y nombres de tablas o restricciones de base de datos. | Se configuró para retornar HTTP 409 Conflict con mensaje sanitizado: *"Conflicto de integridad: ya existe un registro con información duplicada o incompatible en el sistema."* |
| `GenericResponse.java` | Clase | La clase preexistente en el proyecto había sido borrada al depurar clases legadas de `PersonaController`. | Se restauró y completó en `com.proyecto.servicios.model.GenericResponse` con `codigo` (Integer), `mensaje` (String), `data` (Object opcional con `@JsonInclude(NON_NULL)`), constructores y `@Builder`. |
| `ClienteController.java` | Endpoints de búsqueda | Exponía múltiples métodos `GET` con `@PathVariable` (`/curp/{curp}`, `/rfc/{rfc}`, `/email/{email}`) y `@RequestParam`. | Se eliminaron todos los métodos `GET` individuales y se unificó la consulta en `POST /api/v1/clientes/busquedas` con `ClienteFiltroRequestDTO` y `@Valid @RequestBody`. |
| `ClienteController.java` | Baja de clientes | No existía endpoint de borrado lógico para usuarios/clientes. | Se implementó `PATCH /api/v1/clientes/baja` recibiendo `ClienteBajaRequestDTO` con `@Valid @RequestBody`. |
| `PersonaController.java` | Clase completa | Controlador redundante con modelo paralelo `sf.Personas` que admitía actualización general y borrado no estándar. | Se eliminó `PersonaController` y todo su árbol de persistencia y servicio (`sf.Personas`, `PersonasServiceImpl`, etc.). |
| `GlobalExceptionHandlerTest.java` | Suite de pruebas | Verificaba campos obsoletos de `ErrorResponse` (`detalles`, `estado`, `timestamp`) y no comprobaba el contrato `GenericResponse` ni el formato `Datos no validos:`. | Se rediseñó la suite con 26 pruebas que verifican exhaustivamente `GenericResponse`, código 1, formato de mapa de errores, validaciones individuales y compuestas. |
| `ClienteControllerTest.java` | `darDeBaja_IdInvalido_Retorna400` | Esperaba el formato previo `ErrorResponse.detalles`. | Se actualizó para verificar `GenericResponse` con `codigo = 1` y mensaje conteniendo `"Datos no validos:"` e `"id"`. |

---

## C. Cambios en ValidadorCampos o GlobalExceptionHandler

### 1. Estrategia arquitectónica elegida
En estricto apego a la Sección 7 de las especificaciones ("Evitar conflictos entre manejadores"), se evaluaron las opciones de diseño:
- **Estrategia seleccionada (Opción 1):** Integración completa y unificada del patrón `ValidadorCampos` dentro de [`GlobalExceptionHandler.java`](file:///c:/INGENIERIA_4_AGOSTO-DIC2026/HASSANI/prueba/src/main/java/com/proyecto/servicios/config/GlobalExceptionHandler.java).
- **Razón técnica:** Mantener una única clase `@RestControllerAdvice` centralizada evita ambigüedad de mapeo (`Ambiguous @ExceptionHandler method mapped`) y garantiza un orden de precedencia predecible y uniforme en Spring Boot tanto en producción como en entornos de prueba (`MockMvcBuilders.standaloneSetup`).

### 2. Anotación utilizada
- `@RestControllerAdvice`: elegida porque la aplicación es una API REST pura donde todos los cuerpos de respuesta de error deben serializarse directamente a JSON, garantizando compatibilidad con clientes modernos.

### 3. Implementación del método de validación `dtoValidation`
Siguiendo fielmente la clase de referencia `ValidadorCampos`:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<GenericResponse> dtoValidation(MethodArgumentNotValidException exception) {
    GenericResponse genericResponse = new GenericResponse();
    Map<String, String> listError = new HashMap<>();

    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
        listError.merge(
                fieldError.getField(),
                fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Dato no válido",
                (m1, m2) -> m1 + "; " + m2
        );
    }

    for (ObjectError globalError : exception.getBindingResult().getGlobalErrors()) {
        listError.merge(
                globalError.getObjectName(),
                globalError.getDefaultMessage() != null ? globalError.getDefaultMessage() : "Dato no válido",
                (m1, m2) -> m1 + "; " + m2
        );
    }

    genericResponse.setCodigo(1);
    genericResponse.setMensaje("Datos no validos: " + listError);

    log.warn("Falla de validación en Request DTO: {}", genericResponse.getMensaje());
    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
}
```

### 4. Tabla de manejadores, códigos HTTP y formatos de respuesta

| Excepción capturada | Código HTTP | Atributo `codigo` | Formato del atributo `mensaje` |
|---|---|---|---|
| `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | `"Datos no validos: {campo=mensaje, ...}"` |
| `ConstraintViolationException` | `400 BAD_REQUEST` | `1` | `"Datos no validos: {propiedad=mensaje, ...}"` |
| `HttpMessageNotReadableException` | `400 BAD_REQUEST` | `400` | Mensaje descriptivo sobre JSON malformado o tipo de dato incompatible. |
| `ValidacionNegocioException` | `400 BAD_REQUEST` | `400` | Mensaje de la regla de negocio violada (ej. minoría de edad). |
| `ClienteNoEncontradoException` | `404 NOT_FOUND` | `404` | Mensaje semántico: `"No se encontró el cliente con ID: {id}"`. |
| `CuentaNoEncontradaException` | `404 NOT_FOUND` | `404` | Mensaje semántico de la cuenta no localizada. |
| `UsuarioNoEncontradoException` | `404 NOT_FOUND` | `404` | Mensaje semántico del usuario no localizado. |
| `EntityNotFoundException` | `404 NOT_FOUND` | `404` | Mensaje seguro: `"El recurso solicitado no fue encontrado."` |
| `ClienteYaExisteException` | `409 CONFLICT` | `409` | Mensaje del correo ya registrado. |
| `CurpDuplicadaException` | `409 CONFLICT` | `409` | Mensaje de la CURP ya registrada. |
| `RfcDuplicadoException` | `409 CONFLICT` | `409` | Mensaje del RFC ya registrado. |
| `ClienteYaEliminadoException` | `409 CONFLICT` | `409` | Mensaje: `"El cliente con ID: {id} ya se encuentra dado de baja."` |
| `DataIntegrityViolationException` | `409 CONFLICT` | `409` | Mensaje sanitizado: *"Conflicto de integridad: ya existe un registro con información duplicada o incompatible en el sistema."* (sin trazas SQL). |
| `AccessDeniedException` | `403 FORBIDDEN` | `403` | Mensaje: `"No cuenta con los permisos necesarios para acceder a este recurso."` |
| `CredencialesInvalidasException` | `401 UNAUTHORIZED` | `401` | Mensaje de autenticación inválida. |
| `Exception` (catch-all) | `500 INTERNAL_SERVER_ERROR` | `500` | Mensaje genérico protegido: *"Ocurrió un error inesperado al procesar la solicitud."* (traza registrada en log interno). |

---

## D. Cambios en GenericResponse

### 1. Ubicación y justificación
- **Archivo:** [`src/main/java/com/proyecto/servicios/model/GenericResponse.java`](file:///c:/INGENIERIA_4_AGOSTO-DIC2026/HASSANI/prueba/src/main/java/com/proyecto/servicios/model/GenericResponse.java).
- **Acción:** Se restauró y adaptó la clase preexistente para respetar el contrato del modelo bancario y permitir la serialización exacta requerida por `ValidadorCampos`.

### 2. Estructura y atributos reales
```java
package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer codigo;
    private String mensaje;
    private Object data;

    public GenericResponse(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }
}
```

### 3. Justificación de los atributos
- **`codigo` (Integer):** Proporciona compatibilidad tanto con el código `1` del patrón `ValidadorCampos` para validaciones, como con códigos numéricos semánticos (`400`, `404`, `409`, `500`) para excepciones de negocio y de infraestructura.
- **`mensaje` (String):** Almacena la cadena formateada `"Datos no validos: " + listError` en caso de fallos de validación, o el mensaje seguro correspondiente en las demás excepciones.
- **`data` (Object):** Declarado opcional con `@JsonInclude(JsonInclude.Include.NON_NULL)` para permitir que respuestas exitosas puedan transportar carga útil cuando se requiera, sin contaminar el JSON de error cuando es `null`.

---

## E. Pruebas de GlobalExceptionHandlerTest

La clase [`GlobalExceptionHandlerTest.java`](file:///c:/INGENIERIA_4_AGOSTO-DIC2026/HASSANI/prueba/src/test/java/com/proyecto/servicios/config/GlobalExceptionHandlerTest.java) fue actualizada y ampliada a **26 pruebas automatizadas** rigurosas, verificando tanto la activación real de `@Valid` a través de endpoints con MockMvc como el comportamiento unitario directo del manejador:

### Detalle de escenarios probados y resultados reales:

| # | Método de prueba | Tipo | Excepción provocada | HTTP esperado | Código esperado | Mensaje / Validación comprobada | Resultado |
|---|---|---|---|---|---|---|---|
| 1 | `crearCliente_CamposObligatoriosAusentes_Devuelve400ConGenericResponse` | MockMvc | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | Faltan todos los campos obligatorios. Valida que el mensaje empiece con `"Datos no validos:"` y contenga `nombre`, `curp`, `rfc`, `domicilio`, etc. | **Aprobada** |
| 2 | `crearCliente_CampoObligatorioAusente_Devuelve400` | MockMvc | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | Solicitud válida a excepción del correo ausente. Comprueba detección específica de `correo`. | **Aprobada** |
| 3 | `crearCliente_CorreoInvalido_Devuelve400` | MockMvc | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | Correo sin `@`. Comprueba mensaje con clave `correo`. | **Aprobada** |
| 4 | `crearCliente_LongitudInvalida_Devuelve400` | MockMvc | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | Nombre de longitud menor al mínimo (`min = 2`). Comprueba detección de `nombre`. | **Aprobada** |
| 5 | `crearCliente_MultiplesCamposInvalidos_Devuelve400` | MockMvc | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | Múltiples errores simultáneos (CURP, RFC, sexo, teléfonos). Comprueba presencia de todos los campos en el mapa. | **Aprobada** |
| 6 | `buscarClientes_CriteriosBusquedaInvalidos_Devuelve400` | MockMvc | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | Rango de fechas invertido (`fechaCreacionDesde` > `fechaCreacionHasta`). Comprueba captura de `rangoFechasValido`. | **Aprobada** |
| 7 | `buscarClientes_CurpInvalidaEnFiltro_Devuelve400` | MockMvc | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | CURP malformada en filtro combinable. Comprueba detección de `curp`. | **Aprobada** |
| 8 | `darDeBaja_IdInvalido_Devuelve400` | MockMvc | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | Solicitud de baja con `id: -10`. Comprueba violación de `@Positive` sobre `id`. | **Aprobada** |
| 9 | `handleHttpMessageNotReadable_JsonMalformado_Retorna400` | MockMvc | `HttpMessageNotReadableException` | `400 BAD_REQUEST` | `400` | Cuerpo JSON con sintaxis rota o truncada. | **Aprobada** |
| 10 | `handleValidacionNegocio_Retorna400` | MockMvc | `ValidacionNegocioException` | `400 BAD_REQUEST` | `400` | Regla de negocio de mayoría de edad (< 18 años). | **Aprobada** |
| 11 | `handleClienteNoEncontrado_Retorna404` | MockMvc | `ClienteNoEncontradoException` | `404 NOT_FOUND` | `404` | Consulta o baja de cliente con ID inexistente. | **Aprobada** |
| 12 | `handleClienteYaExiste_Retorna409` | MockMvc | `ClienteYaExisteException` | `409 CONFLICT` | `409` | Intento de registro con correo duplicado. | **Aprobada** |
| 13 | `handleCurpDuplicada_Retorna409` | MockMvc | `CurpDuplicadaException` | `409 CONFLICT` | `409` | Intento de registro con CURP duplicada. | **Aprobada** |
| 14 | `handleClienteYaEliminado_Retorna409` | MockMvc | `ClienteYaEliminadoException` | `409 CONFLICT` | `409` | Intento de baja sobre cliente previamente inactivo. | **Aprobada** |
| 15 | `handleDataIntegrityViolation_Retorna409Seguro` | MockMvc | `DataIntegrityViolationException` | `409 CONFLICT` | `409` | Conflicto de BD; verifica que no exponga sentencias SQL ni nombres de tablas. | **Aprobada** |
| 16 | `handleEntityNotFound_Retorna404` | MockMvc | `EntityNotFoundException` | `404 NOT_FOUND` | `404` | Excepción JPA de entidad no encontrada. | **Aprobada** |
| 17 | `handleGenericException_Retorna500Seguro` | MockMvc | `NullPointerException` | `500 INTERNAL_SERVER_ERROR` | `500` | Error inesperado; comprueba respuesta protegida sin trazas técnicas. | **Aprobada** |
| 18 | `testUnitario_dtoValidation` | Unitaria | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | Ejecución directa del método con dos campos inválidos; comprueba formato exacto del mapa. | **Aprobada** |
| 19 | `testUnitario_dtoValidation_camposRepetidos` | Unitaria | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `1` | Dos violaciones sobre el mismo campo (`nombre`); comprueba concatenación vía `merge` sin pérdida de mensajes. | **Aprobada** |
| 20 | `testUnitario_handleValidacionNegocio` | Unitaria | `ValidacionNegocioException` | `400 BAD_REQUEST` | `400` | Ejecución directa; verifica código y mensaje de negocio. | **Aprobada** |
| 21 | `testUnitario_handleClienteNoEncontrado` | Unitaria | `ClienteNoEncontradoException` | `404 NOT_FOUND` | `404` | Ejecución directa; verifica código 404 y mensaje claro. | **Aprobada** |
| 22 | `testUnitario_handleConflictosExistencia` | Unitaria | `ClienteYaEliminadoException` | `409 CONFLICT` | `409` | Ejecución directa; verifica código 409 y mensaje. | **Aprobada** |
| 23 | `testUnitario_handleDataIntegrityViolation` | Unitaria | `DataIntegrityViolationException` | `409 CONFLICT` | `409` | Sanitización estricta; comprueba ausencia de `SELECT` o `passwords`. | **Aprobada** |
| 24 | `testUnitario_handleGenericException` | Unitaria | `RuntimeException` | `500 INTERNAL_SERVER_ERROR` | `500` | Ocultamiento de trazas; comprueba que `token_123456` no se transmita al cliente. | **Aprobada** |
| 25 | `testUnitario_handleAccesoDenegado` | Unitaria | `AccessDeniedException` | `403 FORBIDDEN` | `403` | Verificación de respuesta HTTP 403. | **Aprobada** |
| 26 | `testUnitario_handleConstraintViolation` | Unitaria | `ConstraintViolationException` | `400 BAD_REQUEST` | `1` | Verificación de código 1 y formato `Datos no validos:`. | **Aprobada** |

---

## F. Pendientes

**Ninguno.**
- El 100% de los requisitos del prompt han sido implementados y comprobados con éxito.
- Las tres operaciones autorizadas (Registro, Consulta con filtros combinables y Borrado lógico) se encuentran completamente operativas, con validación Bean Validation en DTOs, sin `@PathVariable` ni `@RequestParam`, y con manejo global de excepciones estandarizado al estilo `ValidadorCampos` sobre `GenericResponse`.
- La ejecución total de la suite de pruebas del proyecto (`.\gradlew.bat test`) arrojó **79 pruebas ejecutadas y 79 pruebas aprobadas al 100%**, sin fallos ni errores.

---

## G. Métodos reutilizables y sobrecarga implementados

Para garantizar alta cohesión y código limpio, se consolidaron componentes reutilizables y sobrecarga de métodos en la arquitectura:

### 1. Métodos reutilizables
- **`ClienteServiceImpl.obtenerClienteActivo(Long id)`:** Centraliza la búsqueda por clave primaria y verificación del estado activo (`activo = true`), evitando consultas duplicadas y lanzando `ClienteNoEncontradoException` (404) o `ClienteYaEliminadoException` (409).
- **`ClienteServiceImpl.normalizarCorreo(String correo)`:** Centraliza la sanitización canónica de correos (`trim().toLowerCase(Locale.ROOT)`).
- **`ClienteSpecifications.normalizar(String valor)`:** Elimina espacios en blanco y filtra cadenas vacías para evitar predicados SQL innecesarios.

### 2. Sobrecarga de métodos
- **`ClienteService` / `ClienteServiceImpl`:**
  - `List<ClienteResponseDTO> buscarClientes(ClienteFiltroRequestDTO filtro)`: Búsqueda dinámica general mediante DTO y Criteria API.
  - `List<ClienteResponseDTO> buscarClientes(String nombre, String correo)`: Búsqueda sobrecargada simplificada por coincidencia de nombre y correo, delegando internamente en la especificación común.
- **`ClienteSpecifications`:**
  - `Specification<Cliente> construirCriterios(ClienteFiltroRequestDTO filtro)`: Construye predicados dinámicos a partir del DTO.
  - `Specification<Cliente> construirCriterios(String nombre, String correo)`: Construye predicados sobrecargados simplificados.
- **`ClienteMapper`:**
  - `ClienteResponseDTO toDTO(Cliente cliente)`: Mapeo individual de entidad a DTO.
  - `List<ClienteResponseDTO> toDTO(List<Cliente> clientes)`: Mapeo sobrecargado de colecciones con control nulo seguro.
