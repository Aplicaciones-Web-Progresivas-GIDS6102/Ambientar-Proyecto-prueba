# Manual de uso de la API

Guía para iniciar el servicio y probar sus endpoints desde Postman. La colección importable está en `postman_collecciones.json`.

## 1. Preparación

- Inicia PostgreSQL y verifica la configuración local en `src/main/resources/application.properties`. La URL configurada apunta a `localhost:5432/postgres`.
- Ejecuta la aplicación Spring Boot. Por defecto escucha en el puerto `8080`.
- Abre Swagger en `http://localhost:8080/swagger-ui/index.html`.
- En Postman, importa `postman_collecciones.json` con **Import → File**. La variable `baseUrl` vale `http://localhost:8080`; cámbiala si el servidor usa otra dirección.
- Para las solicitudes JSON selecciona **Body → raw → JSON**. Envía `Content-Type: application/json`.

## 2. Orden recomendado

1. Crea un cliente con `POST /api/v1/clientes`. La creación también genera automáticamente una cuenta bancaria y un saldo inicial.
2. Copia el `id` del cliente de la respuesta.
3. Registra sus credenciales con `POST /auth/register`, usando ese `clienteId`.
4. Inicia sesión con `POST /auth/login` y copia el `token` devuelto.
5. Envía las solicitudes protegidas con `Authorization: Bearer <token>`. En Postman también puedes seleccionar **Authorization → Bearer Token** y pegar el token.
6. Usa el ID y el número de cuenta devueltos al crear el cliente para probar las consultas. Deja la baja lógica del cliente para el final.

Las rutas de autenticación, el registro inicial de cliente (`POST /clientes` y `POST /api/v1/clientes`) y GestoPago son públicas. Las demás rutas requieren JWT.

## 3. Clientes

Las rutas de clientes están disponibles tanto bajo `/clientes` como bajo `/api/v1/clientes`. En los ejemplos se usa `/api/v1/clientes`.

### Crear cliente

**POST** `/api/v1/clientes`  
Alternativa: **POST** `/clientes`  
Pública. Responde `201 Created`. Crea al cliente, su domicilio y una cuenta con saldo inicial.

```json
{
  "nombre": "Maria",
  "segundoNombre": "Elena",
  "apellidoPaterno": "Garcia",
  "apellidoMaterno": "Lopez",
  "fechaNacimiento": "1990-01-15",
  "curp": "GODE900115MDFRRL09",
  "rfc": "GODE9001151A2",
  "sexo": "F",
  "nacionalidad": "MEXICANA",
  "estadoCivil": "SOLTERA",
  "correo": "maria.garcia.demo@example.com",
  "telefonoMovil": "5512345678",
  "telefonoAlternativo": "5555551234",
  "informacionLaboral": {
    "ocupacion": "Ingeniera",
    "empresa": "Empresa de ejemplo",
    "ingresoMensual": 25000.00
  },
  "domicilio": {
    "calle": "Avenida Insurgentes",
    "numeroExterior": "123",
    "numeroInterior": "4B",
    "colonia": "Del Valle",
    "municipio": "Benito Juarez",
    "estado": "Ciudad de Mexico",
    "codigoPostal": "03100",
    "pais": "MEXICO"
  }
}
```

Reglas principales:

- La persona debe ser mayor de edad y la fecha va como `YYYY-MM-DD`.
- La CURP debe tener 18 caracteres y el RFC debe tener homoclave: 13 caracteres para persona física o 12 para persona moral. Se valida el formato y la longitud; usa los valores oficiales.
- CURP, RFC y correo deben ser únicos. Cambia los datos de ejemplo si ya existen en la base.
- `sexo` acepta los valores `M`, `F` o `X`.
- El teléfono móvil debe ser de 10 dígitos; el alternativo, si se envía, debe tener entre 10 y 15 dígitos.
- `informacionLaboral` puede omitirse; si se envía, incluye ocupación, empresa e ingreso mensual mayor que cero.
- El domicilio es obligatorio. `codigoPostal` debe tener exactamente 5 dígitos.
- También se aceptan campos laborales planos (`ocupacion`, `empresa`, `ingresoMensual`) y los alias `movil` / `telefonoMovil`.

Los datos del ejemplo son demostrativos: no representan identidades reales ni garantizan que pasen validaciones externas de CURP/RFC.

### Listar clientes activos

**GET** `/api/v1/clientes` — requiere JWT. Devuelve los clientes activos. Sin cuerpo.

### Obtener cliente por ID

**GET** `/api/v1/clientes/{id}` — requiere JWT. Sustituye `{id}` por el ID numérico del cliente. Sin cuerpo.

### Actualizar cliente

**PUT** `/api/v1/clientes/{id}` — requiere JWT. Recibe el mismo formato JSON que el registro. Mantén la CURP y RFC originales; no se permite cambiarlos.

```json
{
  "nombre": "Maria",
  "segundoNombre": "Elena",
  "apellidoPaterno": "Garcia",
  "apellidoMaterno": "Lopez",
  "fechaNacimiento": "1990-01-15",
  "curp": "GODE900115MDFRRL09",
  "rfc": "GODE9001151A2",
  "sexo": "F",
  "nacionalidad": "MEXICANA",
  "estadoCivil": "SOLTERA",
  "correo": "maria.garcia.demo@example.com",
  "telefonoMovil": "5512345678",
  "domicilio": {
    "calle": "Avenida Insurgentes",
    "numeroExterior": "123",
    "colonia": "Del Valle",
    "municipio": "Benito Juarez",
    "estado": "Ciudad de Mexico",
    "codigoPostal": "03100",
    "pais": "MEXICO"
  }
}
```

### Dar de baja cliente

**DELETE** `/api/v1/clientes/{id}` — requiere JWT. Es una baja lógica; también desactiva sus cuentas. No lleva cuerpo y responde `204 No Content`.

### Buscar por CURP

- **GET** `/api/v1/clientes/curp/{curp}`
- Alternativa: **GET** `/api/v1/clientes/busquedas/curp/{curp}`

Requiere JWT. Sin cuerpo.

### Buscar por RFC

- **GET** `/api/v1/clientes/rfc/{rfc}`
- Alternativa: **GET** `/api/v1/clientes/busquedas/rfc/{rfc}`

Requiere JWT. Sin cuerpo.

### Buscar por correo

Opciones equivalentes:

- **GET** `/api/v1/clientes/email/{email}`
- **GET** `/api/v1/clientes/correo/{email}`
- **GET** `/api/v1/clientes/busquedas/correo?correo={email}`

Requiere JWT. En Postman, usa Params para el parámetro `correo` en la tercera ruta; no se envía JSON.

### Buscar por rango de creación

**GET** `/api/v1/clientes/busquedas/fecha-creacion?fechaInicio={inicio}&fechaFin={fin}` — requiere JWT. Fechas ISO 8601, por ejemplo:

```text
fechaInicio=2026-01-01T00:00:00
fechaFin=2026-12-31T23:59:59
```

Sin cuerpo.

## 4. Autenticación

### Registrar usuario para un cliente

**POST** `/auth/register` — pública. El cliente debe existir y no tener ya un usuario. El `username` debe ser único y tener entre 4 y 50 caracteres; `password`, entre 6 y 100.

```json
{
  "clienteId": 1,
  "username": "usuario_demo",
  "password": "ClaveDemo123"
}
```

### Iniciar sesión

**POST** `/auth/login` — pública. Si las credenciales son correctas, responde con un JWT.

```json
{
  "username": "usuario_demo",
  "password": "ClaveDemo123"
}
```

Copia el campo `token` de la respuesta. En endpoints protegidos envíalo como `Authorization: Bearer <token>`. El token tiene una duración informada de 24 horas; respeta también las políticas de bloqueo por intentos fallidos.

## 5. Cuentas bancarias

Todas estas consultas requieren JWT y no llevan cuerpo. Usa el número de cuenta generado al crear el cliente.

| Método | Ruta | Uso |
| --- | --- | --- |
| GET | `/cuentas/{numeroCuenta}` | Consultar una cuenta por su número |
| GET | `/cuentas/{numeroCuenta}/saldo` | Consultar el saldo de una cuenta |
| GET | `/cuentas/activas` | Listar cuentas activas |
| GET | `/cuentas/cliente/{clienteId}` | Listar las cuentas de un cliente |

## 6. Biometría

Usa exclusivamente metadatos ficticios de prueba; no envíes plantillas, vectores ni muestras biométricas reales.

### Registrar metadatos biométricos

**POST** `/biometria` — requiere JWT. `clienteId` debe corresponder a un cliente y `tipoBiometria` debe ser uno de los valores indicados.

```json
{
  "clienteId": 1,
  "tipoBiometria": "HUELLA_DACTILAR",
  "vectorCaracteristicas": "VECTOR_DEMO_NO_REAL",
  "hashBiometrico": "HASH_DEMO_NO_REAL",
  "algoritmo": "DEMO",
  "puntuacionCalidad": 0.95,
  "activo": true
}
```

Valores admitidos para `tipoBiometria`: `HUELLA_DACTILAR`, `RECONOCIMIENTO_FACIAL`, `IRIS`, `PATRON_VOZ`.

### Consultar registros biométricos por cliente

**GET** `/biometria/cliente/{clienteId}` — requiere JWT. Sin cuerpo.

### Desactivar registro biométrico

**PUT** `/biometria/{id}/desactivar` — requiere JWT. Sin cuerpo. Sustituye `{id}` por el ID del registro biométrico.

## 7. Personas

Estos endpoints requieren JWT. La operación de eliminación está implementada como `PUT`, no como `DELETE`.

### Crear persona

**POST** `/personas`

```json
{
  "nombre": "Ana",
  "apellidoP": "Garcia",
  "apellidoMaterno": "Lopez"
}
```

### Actualizar persona

**PUT** `/personasActualiza`

```json
{
  "nombre": "Ana",
  "apellidoP": "Garcia",
  "apellidoMaterno": "Lopez"
}
```

### Eliminar persona

**PUT** `/personasElimina`

```json
{
  "nombre": "Ana"
}
```

## 8. GestoPago

### Consultar productos

**POST** `/api/v1/gestopago/productos` — ruta pública en esta API. Envía credenciales válidas del servicio externo configuradas para tu entorno; no uses credenciales reales en archivos que vayas a compartir.

```json
{
  "usuario": "USUARIO_GESTOPAGO",
  "password": "CAMBIAR_POR_CREDENCIAL_VALIDA"
}
```

## 9. Errores frecuentes

| HTTP | Significado habitual |
| --- | --- |
| 400 | JSON inválido, campos faltantes o regla de negocio incumplida |
| 401 | Falta el JWT, expiró o las credenciales de inicio de sesión son incorrectas |
| 403 | El usuario autenticado no tiene permiso |
| 404 | No se encontró el cliente, cuenta o registro solicitado |
| 409 | CURP, RFC, correo o username duplicado; o conflicto de unicidad |
| 500 | Error inesperado del servidor; revisa los logs de Spring Boot para conocer la causa |

Al crear o actualizar un cliente, lee el campo `detalles` de la respuesta 400 para identificar el campo que no pasó la validación. En caso de `500`, el mensaje HTTP es genérico: los logs de la aplicación contienen la excepción concreta.
