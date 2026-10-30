package com.proyecto.servicios.dto;

import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PagoServicioRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private PagoServicioRequest valido() {
        return PagoServicioRequest.builder().numCuenta("4000123456").idServicio("2284").idProducto("14302")
                .monto(new BigDecimal("100.00")).telefono("5512345678").codigo2FA("907531").build();
    }

    private Set<String> camposInvalidos(PagoServicioRequest r) {
        return validator.validate(r).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void requestCompletoValido() {
        assertThat(validator.validate(valido())).isEmpty();
    }

    @Test
    void montoCeroONegativo() {
        PagoServicioRequest r = valido();
        r.setMonto(BigDecimal.ZERO);
        assertThat(camposInvalidos(r)).contains("monto");
        r.setMonto(new BigDecimal("-1"));
        assertThat(camposInvalidos(r)).contains("monto");
    }

    @Test
    void montoConMasDeDosDecimales() {
        PagoServicioRequest r = valido();
        r.setMonto(new BigDecimal("10.001"));
        assertThat(camposInvalidos(r)).contains("monto");
    }

    @Test
    void camposObligatorios() {
        Set<String> campos = camposInvalidos(new PagoServicioRequest());
        assertThat(campos).contains("numCuenta", "idServicio", "idProducto", "monto", "codigo2FA", "telefonoOReferenciaPresente");
    }

    @Test
    void formatosInvalidos() {
        PagoServicioRequest r = valido();
        r.setNumCuenta("abc");
        r.setTelefono("123");
        r.setReferencia("ref inválida!");
        r.setCodigo2FA("12a");
        assertThat(camposInvalidos(r)).contains("numCuenta", "telefono", "referencia", "codigo2FA");
    }

    @Test
    void soloReferenciaEsValido() {
        PagoServicioRequest r = valido();
        r.setTelefono(null);
        r.setReferencia("REF-123");
        Set<ConstraintViolation<PagoServicioRequest>> v = validator.validate(r);
        assertThat(v).isEmpty();
    }

    @Test
    void toStringNoExponeCodigo2FA() {
        assertThat(valido().toString()).doesNotContain("907531");
    }
}
