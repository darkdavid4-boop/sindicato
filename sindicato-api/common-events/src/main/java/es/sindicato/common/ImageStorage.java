package es.sindicato.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Component
public class ImageStorage {
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 40_000_000;

    private final Path uploadDirectory;

    public ImageStorage(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile image, String collection) {
        if (image == null || image.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debes adjuntar una imagen");
        }
        if (image.getSize() > MAX_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen no puede superar los 5 MB");
        }

        try {
            byte[] content = image.getBytes();
            String extension = validateImage(content);
            String filename = UUID.randomUUID() + "." + extension;
            Files.createDirectories(uploadDirectory);
            Files.write(uploadDirectory.resolve(filename), content);
            return "/media/" + collection + "/" + filename;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen");
        }
    }

    private String validateImage(byte[] content) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "La resolución de la imagen es demasiado grande");
                }
                return switch (format) {
                    case "jpeg", "jpg" -> "jpg";
                    case "png" -> "png";
                    case "gif" -> "gif";
                    default -> throw invalidImage();
                };
            } finally {
                reader.dispose();
            }
        }
    }

    private ApiException invalidImage() {
        return new ApiException(HttpStatus.BAD_REQUEST, "El fichero debe ser una imagen PNG, JPG o GIF válida");
    }
}
