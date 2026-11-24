package es.sindicato.events;

public record EstadoAfiliadoEvent(Long afiliadoId, String email, String estado) {}
