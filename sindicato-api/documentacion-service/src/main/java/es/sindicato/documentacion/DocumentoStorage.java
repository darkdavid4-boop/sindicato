package es.sindicato.documentacion;

import es.sindicato.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Component
public class DocumentoStorage {
    private static final long MAX_SIZE = 20 * 1024 * 1024;
    private static final long MAX_PIXELS = 40_000_000;
    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path directory;
    private final SecretKeySpec encryptionKey;

    public DocumentoStorage(@Value("${app.storage-dir:uploads/documentos}") String directory,
                            @Value("${app.storage-key:}") String encodedKey) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != 32) {
                throw new IllegalArgumentException("La clave de cifrado debe contener 32 bytes");
            }
            this.encryptionKey = new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Debes configurar APP_DOCUMENTOS_KEY como una clave AES-256 en Base64", ex);
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debes adjuntar un documento");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El documento no puede superar los 20 MB");
        }

        try {
            byte[] content = file.getBytes();
            FileType fileType = detectType(content);
            String key = UUID.randomUUID() + "." + fileType.extension();
            Files.createDirectories(directory);
            Path target = directory.resolve(key).normalize();
            if (!target.getParent().equals(directory)) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el documento");
            }
            Path temporary = Files.createTempFile(directory, ".documento-", ".tmp");
            try {
                Files.write(temporary, encrypt(content));
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                    Files.move(temporary, target);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return new StoredFile(key, fileType.contentType(), content.length, safeFilename(file.getOriginalFilename(), fileType.extension()));
        } catch (IOException | GeneralSecurityException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el documento");
        }
    }

    public byte[] read(String key) {
        try {
            return decrypt(Files.readAllBytes(resolve(key)));
        } catch (IOException | GeneralSecurityException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el documento");
        }
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo eliminar el archivo del documento");
        }
    }

    private Path resolve(String key) {
        Path target = directory.resolve(key).normalize();
        if (!target.getParent().equals(directory)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "La ruta del documento no es válida");
        }
        return target;
    }

    private FileType detectType(byte[] content) throws IOException {
        if (startsWith(content, "%PDF-".getBytes())) {
            return new FileType("pdf", "application/pdf");
        }

        FileType expected;
        if (startsWith(content, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            expected = new FileType("jpg", "image/jpeg");
        } else if (startsWith(content, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            expected = new FileType("png", "image/png");
        } else {
            throw invalidFile();
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw invalidFile();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidFile();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS || !expected.extension().equals(format.equals("jpeg") ? "jpg" : format)) {
                    throw invalidFile();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw invalidFile();
        }
        return expected;
    }

    private boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private byte[] encrypt(byte[] content) throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE_SIZE];
        RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_SIZE, nonce));
        byte[] encrypted = cipher.doFinal(content);
        byte[] result = new byte[nonce.length + encrypted.length];
        System.arraycopy(nonce, 0, result, 0, nonce.length);
        System.arraycopy(encrypted, 0, result, nonce.length, encrypted.length);
        return result;
    }

    private byte[] decrypt(byte[] content) throws GeneralSecurityException {
        if (content.length < NONCE_SIZE + TAG_SIZE / 8) {
            throw new GeneralSecurityException("El archivo cifrado está incompleto");
        }
        byte[] nonce = new byte[NONCE_SIZE];
        System.arraycopy(content, 0, nonce, 0, nonce.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_SIZE, nonce));
        return cipher.doFinal(content, nonce.length, content.length - nonce.length);
    }

    private String safeFilename(String originalFilename, String extension) {
        String name = originalFilename == null ? "" : originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\p{Cntrl}]", "").trim();
        if (name.isBlank()) {
            return "documento." + extension;
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || !extension.equalsIgnoreCase(name.substring(dot + 1))) {
            name = (dot <= 0 ? name : name.substring(0, dot)) + "." + extension;
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private ApiException invalidFile() {
        return new ApiException(HttpStatus.BAD_REQUEST, "El documento debe ser un PDF, JPG o PNG válido");
    }

    public record StoredFile(String key, String contentType, long size, String originalFilename) {
    }

    private record FileType(String extension, String contentType) {
    }
}
