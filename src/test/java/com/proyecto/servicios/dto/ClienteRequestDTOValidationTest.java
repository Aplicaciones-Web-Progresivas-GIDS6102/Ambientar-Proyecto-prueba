package com.proyecto.servicios.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteRequestDTOValidationTest {

    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory;
    }

    @AfterAll
    static void closeValidatorFactory() throws Exception {
        validatorFactory.close();
    }

    @Test
    void acceptsCompletePersonalAndBusinessRfcFormats() {
        assertTrue(validateRfc("RARL051018XXX").isEmpty());
        assertTrue(validateRfc("ABC0510181A2").isEmpty());
    }

    @Test
    void rejectsRfcWithoutHomoclaveWithClearMessage() {
        var violations = validateRfc("RARL051018");

        assertEquals(1, violations.size());
        assertEquals(
                "El RFC debe incluir homoclave: 12 caracteres para persona moral o 13 para persona física.",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void acceptsNullOrEmptySegundoNombre() {
        assertTrue(validateSegundoNombre(null).isEmpty());
        assertTrue(validateSegundoNombre("").isEmpty());
        assertTrue(validateSegundoNombre("Elena").isEmpty());
    }

    @Test
    void rejectsNonNumericTelefonoMovil() {
        var violations = validateTelefonoMovil("llllllllll");
        assertEquals(1, violations.size());
        assertEquals(
                "El teléfono móvil solo debe contener números  de 10 dígitos.",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void acceptsNumericTelefonoMovil() {
        assertTrue(validateTelefonoMovil("4681072996").isEmpty());
        assertTrue(validateTelefonoMovil("5512345678").isEmpty());
    }

    @Test
    void acceptsNumericOrEmptyTelefonoAlternativo() {
        assertTrue(validateTelefonoAlternativo(null).isEmpty());
        assertTrue(validateTelefonoAlternativo("").isEmpty());
        assertTrue(validateTelefonoAlternativo("4681072996").isEmpty());
    }

    @Test
    void rejectsNonNumericTelefonoAlternativo() {
        var violations = validateTelefonoAlternativo("4681072996abc");
        assertEquals(1, violations.size());
        assertEquals(
                "El teléfono alternativo solo debe contener números (de 10 dígitos).",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void validatesSexoCorrectly() {
        var emptyViolations = validateSexo("");
        assertTrue(emptyViolations.stream().anyMatch(v -> v.getMessage().equals("El sexo es obligatorio.")));

        var invalidViolations = validateSexo("INVALIDO");
        assertTrue(invalidViolations.stream().anyMatch(v -> v.getMessage().equals("El sexo es inválido. Debe ser M, F o X.")));

        assertTrue(validateSexo("M").isEmpty());
        assertTrue(validateSexo("F").isEmpty());
    }

    @Test
    void collectsAllValidationErrorsWhenMultipleParametersAreInvalid() {
        var request = ClienteRequestDTO.builder()
                .nombre("a")
                .curp("INVALID_CURP")
                .rfc("INVALID_RFC")
                .sexo("")
                .correo("correo_invalido")
                .telefonoMovil("llllllllll")
                .telefonoAlternativo("4681072996abc")
                .build();

        var violations = validator.validate(request);
        assertTrue(violations.size() >= 6, "Debe capturar todos los detalles de validación de los campos inválidos");
    }

    @Test
    void validatesAllInvalidDomicilioFieldsSimultaneously() {
        var domicilioDTO = DomicilioDTO.builder()
                .calle("A")
                .numeroExterior("***")
                .numeroInterior("/**")
                .colonia("D***")
                .municipio("222/*")
                .estado("9***")
                .codigoPostal("kokok")
                .pais("p")
                .build();

        var request = ClienteRequestDTO.builder()
                .domicilio(domicilioDTO)
                .build();

        var violations = validator.validate(request);
        assertTrue(violations.size() >= 7, "Debe reportar los detalles de validación de cada campo inválido del domicilio");
    }

    private static java.util.Set<jakarta.validation.ConstraintViolation<ClienteRequestDTO>> validateRfc(String rfc) {
        var request = ClienteRequestDTO.builder().rfc(rfc).build();
        return validator.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals("rfc"))
                .collect(java.util.stream.Collectors.toSet());
    }

    private static java.util.Set<jakarta.validation.ConstraintViolation<ClienteRequestDTO>> validateSegundoNombre(String segNombre) {
        var request = ClienteRequestDTO.builder().segundoNombre(segNombre).build();
        return validator.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals("segundoNombre"))
                .collect(java.util.stream.Collectors.toSet());
    }

    private static java.util.Set<jakarta.validation.ConstraintViolation<ClienteRequestDTO>> validateTelefonoMovil(String telMovil) {
        var request = ClienteRequestDTO.builder().telefonoMovil(telMovil).build();
        return validator.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals("telefonoMovil"))
                .collect(java.util.stream.Collectors.toSet());
    }

    private static java.util.Set<jakarta.validation.ConstraintViolation<ClienteRequestDTO>> validateTelefonoAlternativo(String telAlt) {
        var request = ClienteRequestDTO.builder().telefonoAlternativo(telAlt).build();
        return validator.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals("telefonoAlternativo"))
                .collect(java.util.stream.Collectors.toSet());
    }

    private static java.util.Set<jakarta.validation.ConstraintViolation<ClienteRequestDTO>> validateSexo(String sexo) {
        var request = ClienteRequestDTO.builder().sexo(sexo).build();
        return validator.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals("sexo"))
                .collect(java.util.stream.Collectors.toSet());
    }
}
