package es.sindicato.afiliados;

import es.sindicato.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {
    @Test
    void signsInAuthenticatedAffiliateAndReturnsItsSessionToken() {
        AfiliadoService afiliadoService = mock(AfiliadoService.class);
        JwtEncoder jwtEncoder = mock(JwtEncoder.class);
        Afiliado afiliado = new Afiliado();
        afiliado.setId(11L);
        afiliado.setNombre("Ana");
        afiliado.setImagenUrl("/media/afiliados/ana.png");
        when(afiliadoService.autenticar("ana@example.com", "clave-segura"))
                .thenReturn(afiliado);
        when(jwtEncoder.encode(any())).thenReturn(Jwt.withTokenValue("token-firmado")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(14400))
                .subject("11")
                .build());

        SessionResponse session = new AuthenticationService(afiliadoService, jwtEncoder, "")
                .iniciarSesion(new LoginRequest("ana@example.com", "clave-segura"));

        assertEquals("token-firmado", session.token());
        assertEquals(11L, session.afiliadoId());
        assertEquals("Ana", session.nombre());
        assertEquals("AFILIADO", session.role());
        assertEquals("/media/afiliados/ana.png", session.imagenUrl());
        assertEquals(14400, session.expiresIn());
        verify(afiliadoService).autenticar("ana@example.com", "clave-segura");
    }

    @Test
    void rejectsInvalidCredentials() {
        AfiliadoService afiliadoService = mock(AfiliadoService.class);
        when(afiliadoService.autenticar("ana@example.com", "incorrecta"))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));

        ApiException error = assertThrows(ApiException.class, () ->
                new AuthenticationService(afiliadoService, mock(JwtEncoder.class), "")
                        .iniciarSesion(new LoginRequest("ana@example.com", "incorrecta")));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
    }

    @Test
    void assignsAdminRoleToConfiguredEmailRegardlessOfCase() {
        AfiliadoService afiliadoService = mock(AfiliadoService.class);
        JwtEncoder jwtEncoder = mock(JwtEncoder.class);
        Afiliado afiliado = new Afiliado();
        afiliado.setId(11L);
        afiliado.setNombre("Ana");
        when(afiliadoService.autenticar("ana@example.com", "clave-segura")).thenReturn(afiliado);
        when(jwtEncoder.encode(any())).thenReturn(Jwt.withTokenValue("token-firmado")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(14400))
                .subject("11")
                .build());

        SessionResponse session = new AuthenticationService(afiliadoService, jwtEncoder, "Ana@Example.com")
                .iniciarSesion(new LoginRequest("ana@example.com", "clave-segura"));

        assertEquals("ADMIN", session.role());
    }
}
