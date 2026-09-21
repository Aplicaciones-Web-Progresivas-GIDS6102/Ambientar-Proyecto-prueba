# Documentación Técnica de Solución: Integración de Servicio Externo GestoPago

**Asignatura**: Ingeniería de Software / Desarrollo de Servicios  
**Alumno**: Francisco Hasaní Barrera Portilla  
**Actividad**: Ambientar Proyecto - Integración con Servicio Externo de Productos  

---

## 1. Resumen Ejecutivo y Objetivo

El presente documento expone la solución técnica implementada en la aplicación Java / Spring Boot para habilitar la integración con el servicio externo de **GestoPago**, específicamente para el consumo del endpoint:

`GET /sistema/service/getProductList.do`

La implementación cumple rigurosamente con los estándares de arquitectura por capas existentes en el proyecto, aplicando principios de Clean Code, Inyección de Dependencias, manejo defensivo de errores sin respuestas nulas, autenticación segura mediante Bearer Token sin hardcodeo de credenciales, registro en bitácora (logging) sin exposición de información sensible y cobertura de pruebas unitarias.

---

## 2. Entregables del Proyecto

A continuación se desglosa la ubicación y descripción técnica de cada uno de los entregables requeridos en la actividad:

| Entregable | Elemento / Archivo Implementado | Descripción |
| :--- | :--- | :--- |
| **1. Configuración de propiedades** | `src/main/resources/application.properties`<br>`build.gradle` | Inyección de propiedades para URL base, endpoints, Bearer Token y timeouts de cliente Feign. Inclusión de librería `jackson-dataformat-xml`. |
| **2. Cliente de integración** | `src/main/java/com/proyecto/servicios/client/GestoPagoProductClient.java` | Interfaz declarativa OpenFeign con encabezado dinámico `Authorization: Bearer <token>` y MediaType XML. |
| **3. Servicio de negocio** | `src/main/java/com/proyecto/servicios/service/GestoPagoProductoService.java`<br>`src/main/java/com/proyecto/servicios/service/Impl/GestoPagoProductoServiceImpl.java` | Lógica de negocio, inyección por constructor, resolución de token, logs de invocación y transformación XML a JSON sin valores nulos. |
| **4. DTOs necesarios** | `src/main/java/com/proyecto/servicios/model/gestopago/GestoPagoProductXmlResponse.java`<br>`src/main/java/com/proyecto/servicios/model/gestopago/ProductoDto.java`<br>`src/main/java/com/proyecto/servicios/model/gestopago/ConsultaProductosRequest.java`<br>`src/main/java/com/proyecto/servicios/model/gestopago/ConsultaProductosResponse.java` | Mapeo de XML de entrada, validación Bean Validation de request (`@Size`, `@NotBlank`) y respuesta estandarizada no nula. |
| **5. Pruebas unitarias** | `src/test/java/com/proyecto/servicios/service/GestoPagoProductoServiceImplTest.java` | Pruebas de servicio con JUnit 5 y Mockito para casos de éxito, timeout (503) y error de autenticación (401). |
| **6. Documentación técnica** | `DOCUMENTACION_ENTREGABLE.md` | Explicación detallada de la arquitectura, decisiones técnicas y evidencia de ejecución. |

---

## 3. Arquitectura y Estructura por Capas

Se mantuvo la estructura de paquetes y convenciones preexistentes en el proyecto:

```text
com.proyecto.servicios
├── client
│   └── GestoPagoProductClient.java
├── config
│   └── GlobalExceptionHandler.java
├── controller
│   └── GestoPagoProductoController.java
├── model
│   └── gestopago
│       ├── ConsultaProductosRequest.java
│       ├── ConsultaProductosResponse.java
│       ├── GestoPagoProductXmlResponse.java
│       └── ProductoDto.java
└── service
    ├── GestoPagoProductoService.java
    └── Impl
        └── GestoPagoProductoServiceImpl.java
```

---

## 4. Decisiones Técnicas e Implementación

### 4.1. Configuración y Autenticación Segura (Sin Hardcodeo y Renovación Automática)
- **Decisión**: Para dar cumplimiento a la restricción de no tener tokens hardcodeados en el código fuente, las propiedades de integración se configuraron dinámicamente en `application.properties`.
- **Automatización del Token (Vigencia 24h)**: Se implementó un proceso en segundo plano con la anotación `@Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}")` en `GestoPagoTokenServiceImpl`. Este servicio invoca periódicamente el endpoint `/sistema/app/jwt-gp/authenticate/` del proveedor con las credenciales configuradas (`id-distribuidor=83`, `codigo-dispositivo=GPS83-TPV-17`, `password=12345678`), obtiene un token nuevo y lo almacena automáticamente en la entidad `GestoPagoToken` en Base de Datos PostgreSQL.
- **Mecanismo de Respaldo (Fallback)**: En la capa de servicio (`GestoPagoProductoServiceImpl`), el método `resolverBearerToken()` busca primero la entidad de token activo en la base de datos y, en caso de no encontrarse, utiliza la propiedad inyectada `gestopago.products.bearer-token` como respaldo defensivo.
- **Formateo de Encabezado**: Se asegura de agregar el prefijo `Bearer ` dinámicamente antes de enviar la solicitud HTTP en el cliente Feign.

### 4.2. Mapeo y Transformación de Formato (XML a JSON)
- **Decisión**: El servicio externo `getProductList.do` retorna una estructura XML (`<RESPONSE><PRODUCTOS><producto .../></PRODUCTOS></RESPONSE>`).
- **Implementación**: Se incorporó la dependencia `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` en `build.gradle`.
- **Estructura XML**: Se creó la clase `GestoPagoProductXmlResponse` con anotaciones de Jackson XML (`@JacksonXmlRootElement`, `@JacksonXmlProperty`, `@JacksonXmlElementWrapper`) para deserializar automáticamente tanto los elementos principales (`<MENSAJE>`), como la lista de nodos `<producto>` y sus atributos XML (`servicio`, `producto`, `precio`, etc.).

### 4.3. Garantía de Cero Nulos (Respuestas No Nulas)
- **Decisión**: Se atendió la indicación estricta de no devolver respuestas o campos nulos en las peticiones.
- **Implementación**:
  - Todas las listas en los DTOs de respuesta se inicializan como colecciones vacías (`new ArrayList<>()`).
  - Las variables numéricas y de tipo cadena se inicializan con cadenas vacías `""` o valores numéricos por defecto `0.0`.
  - Se aplica un mapeo defensivo en la capa de servicio que verifica nulos en cada campo recibido del XML antes de construir el DTO final.

### 4.4. Validaciones de Request y Manejo de Errores (HTTP 200/201)
- **Decisión**: Se implementaron validaciones de Bean Validation en la clase `ConsultaProductosRequest` (por ejemplo, validando que el parámetro de contraseña contenga al menos 7 caracteres con `@Size(min = 7)`).
- **Controlador y Manejo Global**:
  - El controlador `GestoPagoProductoController` expone el endpoint `POST /api/v1/gestopago/productos` y aplica `@Valid @RequestBody`.
  - Se implementó la clase `GlobalExceptionHandler` anotada con `@RestControllerAdvice`.
  - Cuando se detecta un error de validación (`MethodArgumentNotValidException`), se captura la excepción y se construye un objeto `ConsultaProductosResponse` con el código `"400"` y el mensaje explicativo del error (ej. "La contraseña debe tener al menos 7 caracteres."), retornando un estado HTTP `200 OK` para mantener la consistencia solicitada.

### 4.5. Monitoreo y Registro en Logs (Seguridad de Información)
- **Decisión**: Registrar el inicio y fin de las invocaciones al servicio externo usando SLF4J.
- **Seguridad**: Se desarrolló una función auxiliar `enmascararToken(String token)` que oculta los caracteres centrales del token (ej. `qmzAAE...ow==`), impidiendo que credenciales de producción queden registradas en los archivos de log.

---

## 5. Pruebas Unitarias y Cobertura

Se implementó la clase de prueba `GestoPagoProductoServiceImplTest` utilizando **JUnit 5** y **Mockito**. Se configuró la tarea de pruebas en `build.gradle` con `testLogging` para visibilidad de ejecuciones.

### Escenarios Probados:
1. `obtenerProductos_Exitoso`: Simula la respuesta exitosa del cliente Feign con productos en XML, verificando la transformación correcta a DTOs y la lista no nula.
2. `obtenerProductos_ErrorComunicacion_FeignException`: Simula una falla de red o tiempo de espera (Timeout) mediante una excepción `FeignException.ServiceUnavailable`, comprobando que la aplicación responda con código `"503"` sin lanzar errores 500 no controlados.
3. `obtenerProductos_ErrorAutenticacion_Unauthorized`: Simula una falla de autenticación 401 (`FeignException.Unauthorized`), verificando el retorno controlado con código `"401"`.

**Resultado de Ejecución (`.\gradlew.bat test`)**:
```text
GestoPagoProductoServiceImplTest > Debe manejar gracefully un error de comunicación FeignException sin devolver nulos PASSED
GestoPagoProductoServiceImplTest > Debe manejar error 401 Unauthorized devolviendo respuesta estandarizada PASSED
GestoPagoProductoServiceImplTest > Debe obtener la lista de productos exitosamente y mapear correctamente desde el XML PASSED

BUILD SUCCESSFUL in 9s
5 actionable tasks: 3 executed, 2 up-to-date
```

---

## 6. Guía de Pruebas en Postman

Para validar el funcionamiento del endpoint en entorno local:

1. **Método HTTP**: `POST`
2. **URL**: `http://localhost:8090/api/v1/gestopago/productos`
3. **Encabezados (Headers)**: `Content-Type: application/json`
4. **Autenticación en Postman**: `No Auth` (El token es manejado y adjuntado automáticamente por el backend de Spring Boot hacia GestoPago).

### Ejemplos de Peticiones y Respuestas:

#### A. Consulta Exitosa (HTTP 200 OK)
- **Request Body (JSON)**:
  ```json
  {
    "usuario": "usuario_prueba",
    "password": "password123"
  }
  ```
- **Response (JSON)**:
  ```json
  {
    "codigo": "01",
    "mensaje": "Operación realizada con éxito",
    "productos": [
      {
        "servicio": "ABIB",
        "producto": "ABIB 100",
        "precio": 100.0,
        "hasDigitoVerificador": false
      }
    ]
  }
  ```

#### B. Error de Validación (Contraseña < 7 caracteres)
- **Request Body (JSON)**:
  ```json
  {
    "usuario": "usuario_prueba",
    "password": "123"
  }
  ```
- **Response (JSON)**:
  ```json
  {
    "codigo": "400",
    "mensaje": "La contraseña debe tener al menos 7 caracteres.",
    "productos": []
  }
  ```

#### C. Error de Token Expirado (HTTP 401)
- **Response (JSON)**:
  ```json
  {
    "codigo": "401",
    "mensaje": "El token de autenticación de GestoPago ha expirado (válido por 24 horas). Se requiere renovar el token.",
    "productos": []
  }
  ```
