# MANUAL DE USUARIO PASO A PASO: PAGOS DE SERVICIOS Y 2FA (tuto_333)

¡Bienvenido(a)! Este manual está redactado de forma sencilla, clara y detallada para que puedas comprender y probar **cada una de las funciones nuevas** del sistema bancario, incluso si no tienes experiencia previa con el proyecto o con APIs.

Aquí aprenderás **qué hace cada función**, **por qué existe**, **cómo probarla en Postman**, **qué debes enviar en cada petición** y **en qué detalles de la respuesta debes fijarte**.

---

## 📋 Resumen Rápido: ¿Qué es lo que se implementó?

Imagina que estás en la app de tu banco y quieres pagar una recarga o servicio (por ejemplo, telefonía o luz). Para garantizar tu seguridad y la de tu dinero, el sistema realiza tres cosas fundamentales:

1. **Catálogo de Servicios**: Te muestra qué servicios puedes pagar y cuánto cuestan.
2. **Segunda Autenticación (2FA por Correo)**: Antes de cobrarte, el sistema genera una clave secreta de 6 dígitos de un solo uso y te la envía a tu correo personal (**`rmm.che@gmail.com`**) mediante el proveedor **Brevo**.
3. **Pago Seguro con GestoPago**: Cuando ingresas ese código de 6 dígitos, el banco cifra la información con algoritmos de alta seguridad (**AES**), contacta al proveedor **GestoPago** para aplicar el pago, y **únicamente descuenta tu dinero si el proveedor confirma el pago con éxito**. Si algo falla o no hay confirmación, **tu dinero queda 100% protegido e intacto**.

---

## 🔑 Datos de Prueba ya Creados y Listos para Usar

Para que no tengas que crear cuentas ni configurar tablas a mano, el sistema ya incluye una migración automática (`V8__seed_usuario_pruebas.sql`) con los siguientes datos preparados:

| Dato | Valor de Prueba | Para qué sirve |
| :--- | :--- | :--- |
| **Usuario (Login)** | `lizet` | Tu usuario de acceso al banco. |
| **Contraseña** | `Password123!` | Tu contraseña de acceso. |
| **Correo registrado (2FA)** | `rmm.che@gmail.com` | **Aquí te llegará el correo con el código de 6 dígitos.** |
| **Número de Cuenta** | `4000123456` | Tu cuenta bancaria de prueba. |
| **Saldo Inicial** | `$2,500.00 MXN` | Tu saldo disponible antes de pagar. |
| **Servicio a Pagar** | `2284` (ABIB) | El ID del servicio en GestoPago. |
| **Producto a Pagar** | `14302` (ABIB 100) | El producto que cuesta **$100.00 MXN**. |
| **Monto a Pagar** | `100.00` | El monto exacto del producto. |
| **Teléfono de Referencia** | `4771234567` | Número al que se abona el servicio. |

---

## 🛠️ Cómo Importar la Colección en Postman

Ya dejamos preparado un archivo con todas las peticiones listas para usar:

1. Abre **Postman**.
2. Haz clic en el botón superior izquierdo **Import** (o `Ctrl + O`).
3. Selecciona el archivo:
   ```text
   prueba/postman/GestoPago_Pagos.postman_collection.json
   ```
4. Verás aparecer en tu panel izquierdo una carpeta llamada **`GestoPago - Pagos de Servicios y 2FA`** con 10 peticiones ordenadas paso a paso.
5. Asegúrate de que tu aplicación Spring Boot esté encendida (escuchando en `http://localhost:8080`).

---

## 🚶 Flujo de Prueba Paso a Paso

---

### PASO 1: Iniciar Sesión (Login) y Obtener tu Llave de Seguridad (JWT)

* **¿Por qué se hace?**
  Para operar en el banco, el sistema debe saber quién eres. Al iniciar sesión, el servidor te entrega un **Token JWT** (un pase digital seguro). Todas las operaciones de pago te pedirán este pase.
* **Nombre en Postman**: `1. Iniciar Sesión (Login - Obtener JWT)`
* **Método y URL**: `POST http://localhost:8080/auth/login`
* **Cuerpo de la Petición (Body)**:
  ```json
  {
    "username": "lizet",
    "password": "Password123!"
  }
  ```
* **¿Qué debes ver en la respuesta?**
  - **Código HTTP**: `200 OK`
  - En la respuesta verás un campo `"token"` largo:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "username": "lizet",
      "clienteId": 1,
      "expiresInSeconds": 86400
    }
    ```
* **¿De qué debes darte cuenta?**
  - La colección de Postman tiene un script automático que **guarda ese token solo**. ¡Ya no tienes que copiarlo y pegarlo a mano en las demás peticiones!

---

### PASO 2: Consultar el Catálogo de Productos Disponibles

* **¿Por qué se hace?**
  Para conocer qué servicios ofrece el proveedor GestoPago y validar cuánto cuesta cada producto. Este endpoint es público.
* **Nombre en Postman**: `2. Consultar Catálogo de Productos (Público)`
* **Método y URL**: `GET http://localhost:8080/api/v1/gestopago/productos`
* **¿Qué debes ver en la respuesta?**
  - **Código HTTP**: `200 OK`
  - Una lista de productos donde verás nuestro producto de prueba:
    ```json
    {
      "codigo": "01",
      "mensaje": "Productos consultados exitosamente",
      "data": [
        {
          "servicio": "ABIB",
          "producto": "ABIB 100",
          "idServicio": "2284",
          "idProducto": "14302",
          "precio": 100.0
        }
      ]
    }
    ```

---

### PASO 3: Verificar tu Saldo Antes de Pagar

* **¿Por qué se hace?**
  Para tener constancia de cuánto dinero tienes antes de autorizar cualquier cobro.
* **Nombre en Postman**: `3. Consultar Saldo Actual de la Cuenta`
* **Método y URL**: `GET http://localhost:8080/cuentas/4000123456/saldo`
* **¿Qué debes ver en la respuesta?**
  - **Código HTTP**: `200 OK`
  - Tu saldo disponible debe ser:
    ```json
    {
      "numeroCuenta": "4000123456",
      "saldoDisponible": 2500.00,
      "saldoContable": 2500.00,
      "moneda": "MXN"
    }
    ```

---

### PASO 4: Solicitar tu Código de Segunda Autenticación (2FA)

* **¿Por qué se hace?**
  Antes de que se envíe dinero a GestoPago, el sistema valida que tu cuenta esté activa, que tengas saldo suficiente y que el producto exista. Si todo está bien, **genera un código aleatorio de 6 dígitos** y te lo manda por correo a **`rmm.che@gmail.com`** usando Brevo.
* **Nombre en Postman**: `4. Solicitar Código 2FA para Pago (Envía correo a rmm.che@gmail.com)`
* **Método y URL**: `POST http://localhost:8080/api/v1/gestopago/pagos/solicitar-2fa`
* **Cuerpo de la Petición (Body)**:
  ```json
  {
    "numCuenta": "4000123456",
    "idServicio": "2284",
    "idProducto": "14302",
    "monto": 100.00,
    "telefono": "4771234567"
  }
  ```
* **¿Qué debes ver en la respuesta?**
  - **Código HTTP**: `200 OK`
  - Mensaje de confirmación:
    ```json
    {
      "codigo": 0,
      "mensaje": "Código de segunda autenticación enviado con éxito al correo registrado.",
      "data": {
        "vigenciaSegundos": 300,
        "maxIntentos": 3
      }
    }
    ```
* **¿De qué debes darte cuenta?**
  - El código tiene **5 minutos de vigencia** (300 segundos).
  - Tienes un máximo de **3 intentos** para escribirlo bien.
  - El código queda **amarrado a este pago exacto** (cuenta `4000123456`, producto `14302`, monto `$100.00`). No servirá para pagar otra cosa ni otra cantidad.

---

### PASO 5: Revisar tu Bandeja de Correo Electrónico

1. Abre tu correo **`rmm.che@gmail.com`**.
2. Revisa la bandeja de entrada (o la carpeta de Spam/Promociones si es la primera vez).
3. Verás un correo de **Banco GestoPago** con un contenido como este:
   > *Estimado(a) LIZET,*
   > 
   > *Su código de segunda autenticación (2FA) para autorizar el pago del servicio (ID: 2284, Producto: 14302) por un monto de $100.00 MXN es:*
   > 
   > **[654321]**
   > 
   > *Este código es de uso único y tiene una vigencia de 5 minutos.*
4. **Copia los 6 dígitos.**

---

### PASO 6: Ejecutar el Pago de Servicio con el Código 2FA

* **¿Por qué se hace?**
  Ahora envías la orden definitiva de pago. El sistema verifica tu código de 6 dígitos, cifra los datos bancarios con **AES**, se comunica con el servidor de GestoPago (`sendTx.do`) y aplica el pago.
* **Nombre en Postman**: `5. Pagar Servicio con Código 2FA`
* **Método y URL**: `POST http://localhost:8080/api/v1/gestopago/pagos`
* **Cuerpo de la Petición (Body)**:
  *(Reemplaza `654321` con el código real que llegó a tu correo)*
  ```json
  {
    "numCuenta": "4000123456",
    "idServicio": "2284",
    "idProducto": "14302",
    "monto": 100.00,
    "telefono": "4771234567",
    "codigo2FA": "654321"
  }
  ```
* **¿Qué debes ver en la respuesta?**
  - **Código HTTP**: `200 OK`
  - En el cuerpo:
    ```json
    {
      "codigo": 0,
      "mensaje": "Pago de servicio aplicado exitosamente.",
      "data": {
        "estado": "APLICADO",
        "monto": 100.00,
        "upc": "UPC1730320492123",
        "mensaje": "Transacción aplicada exitosamente."
      }
    }
    ```
* **¿De qué debes darte cuenta?**
  - `"codigo": 0`: Significa éxito bancario.
  - `"estado": "APLICADO"`: El proveedor confirmó que la recarga/pago se procesó.
  - `"upc"`: Es el folio único irrepetible de esta transacción. Postman lo guardará automáticamente para la prueba de conciliación.
  - Tu código 2FA quedó **marcado como consumido**. Si intentas usarlo otra vez, el sistema lo rechazará.

---

### PASO 7: Verificar que tu Saldo SÍ se Descontó

* **¿Por qué se hace?**
  Para constatar la **regla de oro financiera**: el banco únicamente descuenta tu dinero cuando el pago ya está 100% confirmado.
* **Nombre en Postman**: `6. Consultar Saldo Posterior (Verificar Descuento)`
* **Método y URL**: `GET http://localhost:8080/cuentas/4000123456/saldo`
* **¿Qué debes ver en la respuesta?**
  - **Código HTTP**: `200 OK`
  - Tu nuevo saldo debe ser exactamente **`$2,400.00 MXN`**:
    ```json
    {
      "numeroCuenta": "4000123456",
      "saldoDisponible": 2400.00,
      "saldoContable": 2400.00,
      "moneda": "MXN"
    }
    ```
  - Tenías `$2,500.00`, pagaste `$100.00`, ahora tienes `$2,400.00`. ¡El balance cuadra perfecto!

---

### PASO 8: Conciliar Transacciones (confirmTx.do)

* **¿Por qué se hace?**
  ¿Qué pasa si al momento de pagar se corta el internet o el servidor externo tarda más de un minuto en responder?
  El sistema **no te cobra el dinero por si acaso**. Deja la transacción como `PENDIENTE_CONFIRMACION`.
  Con este endpoint puedes preguntarle a GestoPago: *"¿Se aplicó o no se aplicó el folio UPC?"*. Si GestoPago dice `Applied`, entonces sí descuenta el saldo. Si dice `Not Applied`, cancela la operación y no te cobra nada.
* **Nombre en Postman**: `7. Conciliar Transacción Pendiente (confirmTx.do)`
* **Método y URL**: `POST http://localhost:8080/api/v1/gestopago/pagos/confirmar/{{ultimoUpc}}`
* **¿Qué debes ver en la respuesta?**
  - **Código HTTP**: `200 OK`
  - ```json
    {
      "codigo": 0,
      "mensaje": "Resultado de conciliación para UPC UPC1730320492123: APLICADO",
      "data": {
        "upc": "UPC1730320492123",
        "estado": "APLICADO"
      }
    }
    ```

---

## 🛑 Pruebas de Seguridad y Manejo de Errores

Para demostrarte que el sistema no permite fraudes ni errores humanos, prueba las siguientes peticiones incluidas en la colección de Postman:

### 1. Error 401: Código 2FA Incorrecto
- **Petición**: `8. Error 401 - Código 2FA Incorrecto`
- **Body**: Envía el código `"999999"`.
- **Qué debes ver**:
  - **HTTP `401 Unauthorized`**
  - `"mensaje": "El código de segunda autenticación es incorrecto. Intentos restantes: 2."`
  - *Darse cuenta*: El sistema no deja pasar a nadie que no tenga el código real que llegó a tu correo.

### 2. Error 400: Saldo Insuficiente
- **Petición**: `9. Error 400 - Saldo Insuficiente`
- **Body**: Intenta pagar un monto de `$99,999.00`.
- **Qué debes ver**:
  - **HTTP `400 Bad Request`**
  - `"mensaje": "Saldo insuficiente en la cuenta. Disponible: 2400.00, Requerido: 99999.00"`
  - *Darse cuenta*: Ni siquiera se molesta en enviar correos ni llamar a GestoPago si la cuenta no tiene fondos.

### 3. Error 400: Producto Inexistente
- **Petición**: `10. Error 400 - Producto Inexistente en Catálogo`
- **Body**: Intenta pagar un producto con ID `"00000"`.
- **Qué debes ver**:
  - **HTTP `400 Bad Request`**
  - `"mensaje": "El producto o servicio especificado no existe en el catálogo activo de GestoPago."`
  - *Darse cuenta*: Nadie puede inventar identificadores de productos falsos para engañar a la pasarela.

---

## 💡 Glosario de Códigos y Conceptos para Principiantes

- **JWT (JSON Web Token)**: Es una credencial temporal que dice *"este usuario ya puso su usuario y contraseña correctamente y tiene permiso de usar la app por las próximas 24 horas"*.
- **2FA (Two-Factor Authentication)**: Segundo factor de autenticación. Es como cuando el cajero te pide tu tarjeta física (primer factor) y luego te pide tu NIP secreto (segundo factor). Aquí el primer factor es tu contraseña y el segundo factor es el código de tu correo.
- **OTP (One-Time Password)**: Contraseña de un solo uso. Sirve una sola vez y se autodestruye.
- **AES (Advanced Encryption Standard)**: Es el estándar militar de cifrado. Nadie en el camino entre tu computadora y GestoPago puede ver ni robar el número de cuenta ni los montos.
- **UPC**: Identificador único de transacción que exige GestoPago para no duplicar pagos nunca.
- **Brevo**: Plataforma que envía los correos electrónicos transaccionales a tu buzón en segundos.
