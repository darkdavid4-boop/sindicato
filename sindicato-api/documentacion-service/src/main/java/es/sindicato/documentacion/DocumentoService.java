package es.sindicato.documentacion;

import es.sindicato.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class DocumentoService {
    private final DocumentoRepository repository;
    private final DocumentoStorage storage;

    public DocumentoService(DocumentoRepository repository, DocumentoStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional
    public DocumentoResponse crear(Long afiliadoId, DocumentoMetadataRequest datos, MultipartFile archivo) {
        DocumentoStorage.StoredFile stored = storage.store(archivo);
        Documento documento = new Documento();
        documento.setAfiliadoId(afiliadoId);
        documento.setPublicado(true);
        documento.setTipo(datos.tipo());
        documento.setDescripcion(datos.descripcion());
        documento.setNombreArchivo(stored.originalFilename());
        documento.setTipoContenido(stored.contentType());
        documento.setTamano(stored.size());
        documento.setClaveAlmacenamiento(stored.key());
        try {
            return DocumentoResponse.from(repository.save(documento));
        } catch (RuntimeException ex) {
            storage.delete(stored.key());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listar() {
        return repository.findAllByPublicadoTrueOrderByFechaCreacionDesc().stream()
                .map(DocumentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoResponse obtenerPublicado(Long id) {
        return DocumentoResponse.from(buscarPublicado(id));
    }

    @Transactional(readOnly = true)
    public DownloadedDocument descargar(Long id) {
        Documento documento = buscarPublicado(id);
        return new DownloadedDocument(storage.read(documento.getClaveAlmacenamiento()),
                documento.getTipoContenido(), documento.getTamano(), documento.getNombreArchivo());
    }

    @Transactional
    public DocumentoResponse actualizar(Long id, DocumentoMetadataRequest request) {
        Documento documento = buscar(id);
        documento.setTipo(request.tipo());
        documento.setDescripcion(request.descripcion());
        return DocumentoResponse.from(repository.save(documento));
    }

    @Transactional
    public void eliminar(Long id) {
        Documento documento = buscar(id);
        repository.delete(documento);
        repository.flush();
        storage.delete(documento.getClaveAlmacenamiento());
    }

    private Documento buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
    }

    private Documento buscarPublicado(Long id) {
        Documento documento = buscar(id);
        if (!documento.isPublicado()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Documento no encontrado");
        }
        return documento;
    }

    public record DownloadedDocument(byte[] content, String contentType, long size, String filename) {
    }
}
