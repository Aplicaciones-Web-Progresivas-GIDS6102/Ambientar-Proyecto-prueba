package com.proyecto.servicios.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Deserializador flexible de LocalDate para Jackson.
 * Permite capturar fechas mal formadas (ej. "1990-01-99") devolviendo null,
 * evitando que Jackson aborte la lectura del cuerpo JSON completo y permitiendo
 * que Bean Validation (@Valid) ejecute y reporte TODOS los errores del objeto simultáneamente.
 */
@Slf4j
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            log.warn("Formato o valor de fecha de nacimiento inválido recibido en JSON: '{}'", text);
            return null;
        }
    }
}
