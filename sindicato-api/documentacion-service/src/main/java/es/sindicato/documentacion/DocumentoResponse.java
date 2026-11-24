package es.sindicato.documentacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Metadatos de un documento almacenado")
public record DocumentoResponse(
        Long id,
        boolean publicado,
        TipoDocumento tipo,
        String descripcion,
        String nombreArchivo,
        String tipoContenido,
        long tamano,
        Instant fechaCreacion) {

    static DocumentoResponse from(Documento documento) {
        return new DocumentoResponse(documento.getId(), documento.isPublicado(), documento.getTipo(),
                documento.getDescripcion(), documento.getNombreArchivo(), documento.getTipoContenido(),
                documento.getTamano(), documento.getFechaCreacion());
    }
}
