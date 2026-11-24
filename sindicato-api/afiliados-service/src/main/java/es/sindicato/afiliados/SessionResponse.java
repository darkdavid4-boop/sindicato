package es.sindicato.afiliados;

public record SessionResponse(String token, long expiresIn, Long afiliadoId, String role, String nombre, String imagenUrl) {}
