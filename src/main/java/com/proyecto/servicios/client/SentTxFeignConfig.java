package com.proyecto.servicios.client;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Configuración Feign específica para el cliente SentTx.
 * Timeouts más amplios (65 s de lectura) para acomodar la latencia del proveedor de pagos.
 * Esta clase NO debe llevar @Configuration para evitar que se aplique globalmente.
 */
public class SentTxFeignConfig {

    @Value("${gestopago.senttx.connect-timeout-ms:5000}")
    private long connectTimeoutMs;

    @Value("${gestopago.senttx.read-timeout-ms:65000}")
    private long readTimeoutMs;

    @Bean
    public Request.Options sentTxRequestOptions() {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS, readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }
}
