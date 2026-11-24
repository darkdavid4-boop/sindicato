package es.sindicato.actividades;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ParticipacionRequest(@NotNull @Positive Long afiliadoId) {}
