package es.sindicato.documentacion;

import es.sindicato.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoStorageTest {
    private static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @TempDir
    Path uploadDirectory;

    @Test
    void storesPdfWithOpaqueFilenameAndDetectedContentType() throws Exception {
        byte[] pdf = "%PDF-1.7\ncontenido".getBytes();
        DocumentoStorage storage = new DocumentoStorage(uploadDirectory.toString(), ENCRYPTION_KEY);

        DocumentoStorage.StoredFile stored = storage.store(
                new MockMultipartFile("archivo", "..\\privado.pdf", "text/plain", pdf));

        assertEquals("application/pdf", stored.contentType());
        assertEquals("privado.pdf", stored.originalFilename());
        assertTrue(stored.key().matches("[0-9a-f-]+\\.pdf"));
        assertTrue(Files.exists(uploadDirectory.resolve(stored.key())));
        assertTrue(!java.util.Arrays.equals(pdf, Files.readAllBytes(uploadDirectory.resolve(stored.key()))));
        assertTrue(java.util.Arrays.equals(pdf, storage.read(stored.key())));
    }

    @Test
    void rejectsSpoofedMimeTypesAndInvalidFiles() {
        DocumentoStorage storage = new DocumentoStorage(uploadDirectory.toString(), ENCRYPTION_KEY);
        MockMultipartFile invalid = new MockMultipartFile("archivo", "prueba.pdf", "application/pdf", "no es PDF".getBytes());

        ApiException error = assertThrows(ApiException.class, () -> storage.store(invalid));

        assertEquals(400, error.getStatus().value());
    }

    @Test
    void rejectsFilesOverTheUploadLimit() {
        DocumentoStorage storage = new DocumentoStorage(uploadDirectory.toString(), ENCRYPTION_KEY);
        MockMultipartFile oversized = new MockMultipartFile("archivo", "grande.pdf", "application/pdf", new byte[20 * 1024 * 1024 + 1]);

        ApiException error = assertThrows(ApiException.class, () -> storage.store(oversized));

        assertEquals(400, error.getStatus().value());
    }
}
