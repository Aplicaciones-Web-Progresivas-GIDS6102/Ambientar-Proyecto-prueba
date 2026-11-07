package com.proyecto.servicios.service;

import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generador de identificadores únicos UPC (Unique Payment Code) para transacciones de GestoPago.
 * Cumple con:
 * - Generador de cadenas aleatorias de alta entropía (SecureRandom).
 * - Caracteres alfanuméricos en mayúsculas.
 * - Longitud estándar y unicidad garantizada en base de datos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UpcGenerator {

    private static final String ALFANUMERICO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int LONGITUD_ALEATORIA = 16;
    private static final DateTimeFormatter TIMESTAMP_FORMATO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final GestoPagoTransaccionRepository transaccionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Genera un UPC único combinando prefijo temporal de alta resolución y sufijo criptográfico aleatorio.
     * Ejemplo: GP20261030132800A8F29K1L4M7N2P9Q
     */
    public String generarUpc() {
        String upc;
        int intentos = 0;
        do {
            StringBuilder sb = new StringBuilder("GP");
            sb.append(LocalDateTime.now().format(TIMESTAMP_FORMATO));
            for (int i = 0; i < LONGITUD_ALEATORIA; i++) {
                int index = secureRandom.nextInt(ALFANUMERICO.length());
                sb.append(ALFANUMERICO.charAt(index));
            }
            upc = sb.toString();
            intentos++;
            if (intentos > 10) {
                // Fallback de ultra-unicidad
                upc = "GP" + java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
                break;
            }
        } while (transaccionRepository.existsByUpc(upc));

        log.debug("UPC generado exitosamente: {}", upc);
        return upc;
    }
}
