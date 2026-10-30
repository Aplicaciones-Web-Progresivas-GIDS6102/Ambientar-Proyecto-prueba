package com.proyecto.servicios.service;

/**
 * Servicio de cifrado/descifrado AES para la comunicación con el proveedor SentTx.
 * La implementación usa AES/CBC/PKCS5Padding con clave e IV configurados externamente.
 */
public interface AesCipherService {

    /**
     * Cifra el texto plano y devuelve el resultado codificado en Base64.
     *
     * @param textoPlano cadena a cifrar (UTF-8).
     * @return texto cifrado en Base64.
     */
    String cifrar(String textoPlano);

    /**
     * Descifra un texto previamente cifrado y codificado en Base64.
     *
     * @param textoCifrado cadena en Base64 a descifrar.
     * @return texto plano original (UTF-8).
     */
    String descifrar(String textoCifrado);
}
