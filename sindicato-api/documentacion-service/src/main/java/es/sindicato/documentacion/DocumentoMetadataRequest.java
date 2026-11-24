package es.sindicato.documentacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Tipo y descripción del documento")
public record DocumentoMetadataRequest(
        @Schema(description = "Categoría del documento", example = "CONTRATO_ALQUILER")
        @NotNull TipoDocumento tipo,
        @Schema(description = "Descripción opcional", example = "Contrato firmado en enero")
        @Size(max = 500) String descripcion) {
}
