package es.sindicato.events;

import java.time.LocalDateTime;

public record NoticiaPublicadaEvent(Long noticiaId, String titulo, String contenido, LocalDateTime fechaPublicacion) {}
