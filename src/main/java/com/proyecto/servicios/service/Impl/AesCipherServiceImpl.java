package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.CifradoException;
import com.proyecto.servicios.service.AesCipherService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * Implementación de cifrado AES/CBC/PKCS5Padding para payloads enviados al proveedor SentTx.
 * <p>
 * Clave e IV se inyectan como cadenas Base64 desde la configuración.
 * La validación en {@link #validarConfiguracion()} garantiza que la aplicación no arranque
 * con credenciales AES malformadas.
 */
@Service
@Slf4j
public class AesCipherServiceImpl implements AesCipherService {

    private static final String ALGORITMO = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";
    private static final Set<Integer> TAMANIOS_CLAVE_VALIDOS = Set.of(16, 24, 32);

    @Value("${gestopago.aes.key:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}")
    private String aesKeyBase64;

    @Value("${gestopago.aes.iv:MDEyMzQ1Njc4OWFiY2RlZg==}")
    private String aesIvBase64;

    private byte[] keyBytes;
    private byte[] ivBytes;

    /**
     * Valida que la clave y el IV tengan el tamaño correcto para AES.
     * Si falla, la aplicación no arranca y el pago nunca se ejecuta sin cifrado.
     */
    @PostConstruct
    public void validarConfiguracion() {
        try {
            keyBytes = Base64.getDecoder().decode(aesKeyBase64);
            ivBytes = Base64.getDecoder().decode(aesIvBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "La clave o IV de AES no son Base64 válido. Verifique gestopago.aes.key y gestopago.aes.iv.", e);
        }

        if (!TAMANIOS_CLAVE_VALIDOS.contains(keyBytes.length)) {
            throw new IllegalStateException(
                    "La clave AES debe tener 16, 24 o 32 bytes (AES-128/192/256). Tamaño actual: " + keyBytes.length);
        }
        if (ivBytes.length != 16) {
            throw new IllegalStateException(
                    "El IV de AES debe tener exactamente 16 bytes. Tamaño actual: " + ivBytes.length);
        }
        log.info("Configuración AES validada correctamente (clave de {} bits).", keyBytes.length * 8);
    }

    @Override
    public String cifrar(String textoPlano) {
        if (textoPlano == null) {
            throw new IllegalArgumentException("El texto a cifrar no puede ser nulo.");
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            String resultado = Base64.getEncoder().encodeToString(cifrado);

            log.debug("Cifrado AES completado. Longitud del resultado Base64: {}", resultado.length());
            return resultado;
        } catch (Exception e) {
            log.error("Error en cifrado AES: {}", e.getMessage());
            throw new CifradoException("Error en cifrado AES", e);
        }
    }

    @Override
    public String descifrar(String textoCifrado) {
        if (textoCifrado == null) {
            throw new IllegalArgumentException("El texto a descifrar no puede ser nulo.");
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decoded = Base64.getDecoder().decode(textoCifrado);
            byte[] descifrado = cipher.doFinal(decoded);

            log.debug("Descifrado AES completado.");
            return new String(descifrado, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error en descifrado AES: {}", e.getMessage());
            throw new CifradoException("Error en descifrado AES", e);
        }
    }
}
