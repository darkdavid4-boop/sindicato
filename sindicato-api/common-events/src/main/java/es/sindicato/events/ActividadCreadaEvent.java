package es.sindicato.events;

import java.time.LocalDateTime;

public record ActividadCreadaEvent(Long actividadId, String titulo, String contenido, LocalDateTime fechaPublicacion,
                                   String imagenUrl) {}
