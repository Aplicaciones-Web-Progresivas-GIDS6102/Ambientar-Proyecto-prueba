package com.proyecto.servicios.service;

import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpcGeneratorTest {

    @Mock
    private GestoPagoTransaccionRepository transaccionRepository;

    @InjectMocks
    private UpcGenerator upcGenerator;

    @Test
    @DisplayName("Generar UPC único exitosamente")
    void generarUpc_Exitoso() {
        when(transaccionRepository.existsByUpc(anyString())).thenReturn(false);

        String upc = upcGenerator.generarUpc();

        assertThat(upc).isNotBlank();
        assertThat(upc).startsWith("GP");
    }

    @Test
    @DisplayName("Generar UPC con reintento ante colisión y fallback")
    void generarUpc_ConColisionYFallback() {
        // Simular colisiones consecutivas para probar el fallback de UUID
        when(transaccionRepository.existsByUpc(anyString())).thenReturn(true, true, true, true, true, true, true, true, true, true, false);

        String upc = upcGenerator.generarUpc();

        assertThat(upc).isNotBlank();
        assertThat(upc).startsWith("GP");
    }
}
