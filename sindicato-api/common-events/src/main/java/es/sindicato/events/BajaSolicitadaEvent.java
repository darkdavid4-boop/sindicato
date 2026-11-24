package es.sindicato.events;

public record BajaSolicitadaEvent(Long afiliadoId, String nombre, String email, String urlConfirmacion) {}
