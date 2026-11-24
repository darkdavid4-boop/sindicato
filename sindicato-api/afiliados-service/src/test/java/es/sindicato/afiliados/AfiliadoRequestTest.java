package es.sindicato.afiliados;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AfiliadoRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsSpanishDniWithCorrectControlLetter() {
        AfiliadoRequest request = new AfiliadoRequest("Ana", "Pérez", "12345678Z", "ana@example.com", null, null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsSpanishDniWithIncorrectControlLetter() {
        AfiliadoRequest request = new AfiliadoRequest("Ana", "Pérez", "12345678A", "ana@example.com", null, null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMalformedEmail() {
        AfiliadoRequest request = new AfiliadoRequest("Ana", "Pérez", "12345678Z", "correo-invalido", null, null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsOptionalPasswordForLegacyCrudRequests() {
        AfiliadoRequest request = new AfiliadoRequest("Ana", "Pérez", "12345678Z", "ana@example.com", null, null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsPasswordShorterThanTenCharacters() {
        AfiliadoRequest request = new AfiliadoRequest("Ana", "Pérez", "12345678Z", "ana@example.com", null, "corta");
        assertFalse(validator.validate(request).isEmpty());
    }
}
