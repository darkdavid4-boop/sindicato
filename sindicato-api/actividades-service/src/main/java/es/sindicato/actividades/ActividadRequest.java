package es.sindicato.actividades;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ActividadRequest(@NotBlank String titulo, @NotBlank String contenido,
                               @NotNull LocalDateTime fechaPublicacion, @Min(1) int capacidad) {}
