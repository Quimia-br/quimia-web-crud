package com.quimia.quimiawebcrud.service.photo;

import com.quimia.quimiawebcrud.exception.PhotoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Guarda as fotos numa pasta do servidor e as entrega pela rota {@code /uploads/photos/}. É o padrão no computador
 * de quem desenvolve. Em hospedagem que apaga o disco a cada reinício, use o {@link R2PhotoStore}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "disk", matchIfMissing = true)
public class DiskPhotoStore implements PhotoStore {

    private final Path directory;
    private final String publicBaseUrl;

    public DiskPhotoStore(@Value("${app.upload.dir}") String uploadDir,
                          @Value("${app.upload.public-base-url}") String publicBaseUrl) {
        this.directory = Path.of(uploadDir).toAbsolutePath().normalize().resolve("photos");
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    public String save(String fileName, byte[] content, String contentType) {
        Path target = directory.resolve(fileName).normalize();
        if (!target.startsWith(directory)) {
            throw new PhotoException("Não foi possível salvar a foto.");
        }
        try {
            Files.createDirectories(directory);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            log.error("Falha ao gravar a foto em {}", target, e);
            throw new PhotoException("Não foi possível salvar a foto. Tente novamente.");
        }
        return publicBaseUrl + PhotoStorageService.URL_PREFIX + fileName;
    }

    @Override
    public void delete(String fileName) {
        try {
            Files.deleteIfExists(directory.resolve(fileName));
        } catch (IOException e) {
            log.warn("Não foi possível apagar a foto {}", fileName, e);
        }
    }

    @Override
    public Optional<String> ownFileName(String photoUrl) {
        if (photoUrl == null) {
            return Optional.empty();
        }
        String path = photoUrl.startsWith(publicBaseUrl) ? photoUrl.substring(publicBaseUrl.length()) : photoUrl;
        if (!path.startsWith(PhotoStorageService.URL_PREFIX)) {
            return Optional.empty();
        }
        String fileName = path.substring(PhotoStorageService.URL_PREFIX.length());
        return STORED_NAME.matcher(fileName).matches() ? Optional.of(fileName) : Optional.empty();
    }

    @Override
    public Optional<Path> find(String fileName) {
        if (fileName == null || !STORED_NAME.matcher(fileName).matches()) {
            return Optional.empty();
        }
        Path file = directory.resolve(fileName).normalize();
        return file.startsWith(directory) && Files.isRegularFile(file) ? Optional.of(file) : Optional.empty();
    }
}
