package es.sindicato.noticias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record NoticiaRequest(@NotBlank String titulo, @NotBlank String contenido,
                            @NotNull LocalDateTime fechaPublicacion, Long actividadId) {}
