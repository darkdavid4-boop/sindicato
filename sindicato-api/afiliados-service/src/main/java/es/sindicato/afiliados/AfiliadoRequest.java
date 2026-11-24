package es.sindicato.afiliados;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record AfiliadoRequest(
        @NotBlank String nombre,
        @NotBlank String apellidos,
        @NotBlank @Pattern(regexp = "(?i)^[0-9]{8}[A-Z]$|^[XYZ][0-9]{7}[A-Z]$", message = "El DNI/NIE español no tiene un formato válido") String dni,
        @NotBlank @Email String email,
        @Pattern(regexp = "^$|^[+]?[0-9 ()-]{6,20}$", message = "El teléfono no tiene un formato válido") String telefono,
        @Size(min = 10, max = 72, message = "La contraseña debe tener entre 10 y 72 caracteres") String password
) {
    @AssertTrue(message = "La letra del DNI/NIE no es válida")
    public boolean isDniValido() {
        if (dni == null || !dni.matches("(?i)^[0-9]{8}[A-Z]$|^[XYZ][0-9]{7}[A-Z]$")) return false;
        String normalized = dni.toUpperCase(Locale.ROOT);
        String digits = normalized;
        if (normalized.charAt(0) == 'X') digits = "0" + normalized.substring(1);
        else if (normalized.charAt(0) == 'Y') digits = "1" + normalized.substring(1);
        else if (normalized.charAt(0) == 'Z') digits = "2" + normalized.substring(1);
        int number = Integer.parseInt(digits.substring(0, 8));
        return "TRWAGMYFPDXBNJZSQVHLCKE".charAt(number % 23) == normalized.charAt(normalized.length() - 1);
    }

    @AssertTrue(message = "La contraseña no puede superar los 72 bytes")
    public boolean isPasswordLengthValid() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
