package es.sindicato.afiliados;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {
    private static final long SESSION_HOURS = 4;

    private final AfiliadoService afiliadoService;
    private final JwtEncoder jwtEncoder;
    private final Set<String> adminEmails;

    public AuthenticationService(AfiliadoService afiliadoService, JwtEncoder jwtEncoder,
                                 @Value("${app.security.admin-emails:}") String configuredAdminEmails) {
        this.afiliadoService = afiliadoService;
        this.jwtEncoder = jwtEncoder;
        this.adminEmails = Arrays.stream(configuredAdminEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public SessionResponse iniciarSesion(LoginRequest request) {
        Afiliado afiliado = afiliadoService.autenticar(request.email(), request.password());
        String role = adminEmails.contains(request.email().trim().toLowerCase(Locale.ROOT)) ? "ADMIN" : "AFILIADO";
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sindicato")
                .issuedAt(now)
                .expiresAt(now.plus(SESSION_HOURS, ChronoUnit.HOURS))
                .subject(afiliado.getId().toString())
                .claim("name", afiliado.getNombre())
                .claim("authorities", Set.of("ROLE_" + role))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new SessionResponse(token, SESSION_HOURS * 3600, afiliado.getId(), role,
                afiliado.getNombre(), afiliado.getImagenUrl());
    }
}
