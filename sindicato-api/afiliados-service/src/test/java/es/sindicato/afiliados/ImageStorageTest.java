package es.sindicato.afiliados;

import es.sindicato.common.ApiException;
import es.sindicato.common.ImageStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageStorageTest {
    @TempDir
    Path uploadDirectory;

    @Test
    void storesValidImageAndReturnsItsMediaUrl() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        ImageStorage storage = new ImageStorage(uploadDirectory.toString());

        String imageUrl = storage.store(new MockMultipartFile("imagen", "foto.png", "image/png", output.toByteArray()),
                "afiliados");

        assertTrue(imageUrl.matches("/media/afiliados/[0-9a-f-]+\\.png"));
        assertTrue(Files.exists(uploadDirectory.resolve(Path.of(imageUrl).getFileName())));
    }

    @Test
    void rejectsNonImageFilesEvenWhenTheyHaveImageMimeType() {
        ImageStorage storage = new ImageStorage(uploadDirectory.toString());
        MockMultipartFile fakeImage = new MockMultipartFile("imagen", "foto.png", "image/png", "not an image".getBytes());

        ApiException error = assertThrows(ApiException.class, () -> storage.store(fakeImage, "noticias"));

        assertEquals(400, error.getStatus().value());
    }

    @Test
    void rejectsFilesOverTheUploadLimit() {
        ImageStorage storage = new ImageStorage(uploadDirectory.toString());
        MockMultipartFile tooLarge = new MockMultipartFile("imagen", "foto.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        ApiException error = assertThrows(ApiException.class, () -> storage.store(tooLarge, "actividades"));

        assertEquals(400, error.getStatus().value());
    }
}
