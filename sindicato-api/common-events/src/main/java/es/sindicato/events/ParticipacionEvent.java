package es.sindicato.events;

import java.time.LocalDateTime;

public record ParticipacionEvent(Long participacionId, Long actividadId, Long afiliadoId,
                                 LocalDateTime fechaInscripcion, String estado) {}
