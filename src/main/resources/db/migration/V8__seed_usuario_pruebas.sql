-- =============================================================================
-- MIGRACIÓN V8: DATOS DE PRUEBA PARA CLIENTE, USUARIO, CUENTA Y PRODUCTO GESTOPAGO
-- =============================================================================

DO $$
DECLARE
    v_nacionalidad_id BIGINT;
    v_estado_civil_id BIGINT;
    v_estatus_cuenta_id BIGINT;
    v_cliente_id BIGINT;
    v_cuenta_id BIGINT;
BEGIN
    -- 1. Obtener IDs de catálogos base
    SELECT id INTO v_nacionalidad_id FROM paises WHERE codigo_iso = 'MEX' LIMIT 1;
    IF v_nacionalidad_id IS NULL THEN
        INSERT INTO paises (nombre, codigo_iso, activo) VALUES ('MÉXICO', 'MEX', true) RETURNING id INTO v_nacionalidad_id;
    END IF;

    SELECT id INTO v_estado_civil_id FROM estado_civil WHERE descripcion = 'SOLTERO(A)' LIMIT 1;
    IF v_estado_civil_id IS NULL THEN
        INSERT INTO estado_civil (descripcion, activo) VALUES ('SOLTERO(A)', true) RETURNING id INTO v_estado_civil_id;
    END IF;

    SELECT id INTO v_estatus_cuenta_id FROM estatus_cuenta WHERE nombre = 'ACTIVA' LIMIT 1;
    IF v_estatus_cuenta_id IS NULL THEN
        INSERT INTO estatus_cuenta (nombre, descripcion, activo) VALUES ('ACTIVA', 'Cuenta Activa', true) RETURNING id INTO v_estatus_cuenta_id;
    END IF;

    -- 2. Crear Cliente de Prueba (con correo rmm.che@gmail.com)
    SELECT id INTO v_cliente_id FROM clientes WHERE correo = 'rmm.che@gmail.com' LIMIT 1;
    IF v_cliente_id IS NULL THEN
        INSERT INTO clientes (
            nombre, apellido_paterno, apellido_materno, fecha_nacimiento,
            curp, rfc, sexo, nacionalidad_id, estado_civil_id, correo,
            telefono_movil, activo, fecha_creacion, fecha_actualizacion
        ) VALUES (
            'LIZET', 'RAMIREZ', 'HERNANDEZ', '1995-05-15',
            'RAML950515HMCRGR01', 'RAML950515XX1', 'F', v_nacionalidad_id, v_estado_civil_id, 'rmm.che@gmail.com',
            '4771234567', true, NOW(), NOW()
        ) RETURNING id INTO v_cliente_id;
    END IF;

    -- 3. Crear Cuenta Bancaria 4000123456
    SELECT id INTO v_cuenta_id FROM cuentas WHERE numero_cuenta = '4000123456' LIMIT 1;
    IF v_cuenta_id IS NULL THEN
        INSERT INTO cuentas (
            cliente_id, numero_cuenta, estatus_cuenta_id, fecha_creacion, fecha_actualizacion
        ) VALUES (
            v_cliente_id, '4000123456', v_estatus_cuenta_id, NOW(), NOW()
        ) RETURNING id INTO v_cuenta_id;
    END IF;

    -- 4. Crear o Actualizar Saldo Inicial ($2,500.00 MXN)
    IF NOT EXISTS (SELECT 1 FROM saldos WHERE cuenta_id = v_cuenta_id) THEN
        INSERT INTO saldos (
            cuenta_id, saldo_disponible, saldo_contable, moneda, fecha_actualizacion
        ) VALUES (
            v_cuenta_id, 2500.00, 2500.00, 'MXN', NOW()
        );
    END IF;

    -- 5. Crear Usuario 'lizet' con contraseña 'Password123!'
    -- Hash BCrypt: $2a$10$yWyWlKGwGoZkuIZjrKyNGe1Sj8P4p7Um3bxRsV2KS2wQmCS7er8mi
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'lizet') THEN
        INSERT INTO usuarios (
            cliente_id, username, password_hash, activo, intentos_fallidos, bloqueado, fecha_creacion, fecha_actualizacion
        ) VALUES (
            v_cliente_id, 'lizet', '$2a$10$yWyWlKGwGoZkuIZjrKyNGe1Sj8P4p7Um3bxRsV2KS2wQmCS7er8mi',
            true, 0, false, NOW(), NOW()
        );
    END IF;

    -- 6. Insertar Producto GestoPago de Prueba si no existe
    IF NOT EXISTS (SELECT 1 FROM gestopago_productos WHERE id_servicio = '2284' AND id_producto = '14302') THEN
        INSERT INTO gestopago_productos (
            servicio, producto, id_servicio, id_producto, id_cat_tipo_servicio,
            tipo_front, has_digito_verificador, precio, show_ayuda, tipo_referencia,
            legend, fecha_actualizacion
        ) VALUES (
            'ABIB', 'ABIB 100', '2284', '14302', '13',
            '1', false, 100.00, false, 'a',
            'Pago telefónico ABIB 100', NOW()
        );
    END IF;

END $$;
