package es.sindicato.documentacion;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Tag(name = "Documentos", description = "Publicación y descarga pública de documentos del sindicato")
public class DocumentoController {
    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publicar documento", description = "Adjunta y publica un PDF, JPG o PNG (máximo 20 MB). Disponible para afiliados activos y administradores.")
    @ApiResponse(responseCode = "201", description = "Documento creado",
            content = @Content(schema = @Schema(implementation = DocumentoResponse.class)))
    public ResponseEntity<DocumentoResponse> crear(@RequestPart("datos") @Valid DocumentoMetadataRequest datos,
                                                    @RequestPart("archivo") MultipartFile archivo,
                                                    @AuthenticationPrincipal Jwt jwt) {
        DocumentoResponse creado = service.crear(Long.valueOf(jwt.getSubject()), datos, archivo);
        return ResponseEntity.created(URI.create("/api/documentos/" + creado.id())).body(creado);
    }

    @GetMapping("/api/documentos")
    @Operation(summary = "Listar documentos publicados", description = "Devuelve los metadatos públicos de los documentos publicados.")
    @ApiResponse(responseCode = "200", description = "Listado de documentos")
    public List<DocumentoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/api/documentos/{id}")
    @Operation(summary = "Consultar documento", description = "Obtiene los metadatos de un documento.")
    @ApiResponse(responseCode = "200", description = "Metadatos del documento",
            content = @Content(schema = @Schema(implementation = DocumentoResponse.class)))
    public DocumentoResponse obtener(@PathVariable Long id) {
        return service.obtenerPublicado(id);
    }

    @GetMapping("/api/documentos/{id}/archivo")
    @Operation(summary = "Descargar archivo", description = "Descarga un documento publicado.")
    @ApiResponse(responseCode = "200", description = "Archivo del documento",
            content = {
                    @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary"))
            })
    public ResponseEntity<ByteArrayResource> descargar(@PathVariable Long id) {
        DocumentoService.DownloadedDocument documento = service.descargar(id);
        return archivo(documento, ContentDisposition.attachment()
                .filename(documento.filename(), StandardCharsets.UTF_8).build().toString());
    }

    @GetMapping("/api/documentos/{id}/previsualizacion")
    @Operation(summary = "Previsualizar archivo", description = "Muestra en línea un documento publicado.")
    public ResponseEntity<ByteArrayResource> previsualizar(@PathVariable Long id) {
        DocumentoService.DownloadedDocument documento = service.descargar(id);
        return archivo(documento, ContentDisposition.inline()
                .filename(documento.filename(), StandardCharsets.UTF_8).build().toString());
    }

    private ResponseEntity<ByteArrayResource> archivo(DocumentoService.DownloadedDocument documento, String contentDisposition) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documento.contentType()))
                .contentLength(documento.size())
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(new ByteArrayResource(documento.content()));
    }

    @PutMapping("/api/documentos/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar metadatos", description = "Actualiza el tipo y la descripción. Solo para administradores.")
    @ApiResponse(responseCode = "200", description = "Metadatos actualizados",
            content = @Content(schema = @Schema(implementation = DocumentoResponse.class)))
    public DocumentoResponse actualizar(@PathVariable Long id,
                                        @Valid @RequestBody DocumentoMetadataRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/api/documentos/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eliminar documento", description = "Elimina el archivo y sus metadatos. Solo para administradores.")
    @ApiResponse(responseCode = "204", description = "Documento eliminado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
