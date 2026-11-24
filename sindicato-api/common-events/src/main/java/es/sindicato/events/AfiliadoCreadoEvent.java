package es.sindicato.events;

public record AfiliadoCreadoEvent(Long afiliadoId, String nombre, String email, String urlConfirmacion) {}
