package es.sindicato.documentacion;

import es.sindicato.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {
    @Mock
    private DocumentoRepository repository;

    @Mock
    private DocumentoStorage storage;

    private DocumentoService service;

    @BeforeEach
    void setUp() {
        service = new DocumentoService(repository, storage);
    }

    @Test
    void keepsDocumentsThatExistedBeforePublicPublishingHidden() {
        Documento document = new Documento();
        document.setId(17L);
        document.setPublicado(false);
        when(repository.findById(17L)).thenReturn(Optional.of(document));

        ApiException error = assertThrows(ApiException.class, () -> service.obtenerPublicado(17L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test
    void listsOnlyPublishedDocuments() {
        service.listar();

        verify(repository).findAllByPublicadoTrueOrderByFechaCreacionDesc();
    }

    @Test
    void publishesDocumentsAsSoonAsTheyAreUploaded() {
        when(storage.store(any())).thenReturn(new DocumentoStorage.StoredFile(
                "stored.pdf", "application/pdf", 20, "normas.pdf"));
        when(repository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("archivo", "normas.pdf", "application/pdf",
                "%PDF-test".getBytes());

        service.crear(8L, new DocumentoMetadataRequest(TipoDocumento.OTRO, "Normas"), file);

        var saved = org.mockito.ArgumentCaptor.forClass(Documento.class);
        verify(repository).save(saved.capture());
        assertTrue(saved.getValue().isPublicado());
    }
}
