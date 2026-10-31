package com.quimia.quimiawebcrud.service.photo;

import com.quimia.quimiawebcrud.exception.PhotoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guarda as fotos enviadas (usuários e empresas) numa pasta do servidor.
 * <p>
 * Cada foto é validada pelo conteúdo real (não pela extensão), decodificada e gravada de novo pelo servidor
 * (o que descarta metadados e qualquer conteúdo escondido) e salva com nome aleatório.
 */
@Slf4j
@Service
public class PhotoStorageService {

    public static final String URL_PREFIX = "/uploads/photos/";

    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final int MAX_SIDE = 4096;
    private static final long MAX_PIXELS = 16_000_000L;
    private static final Pattern STORED_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)$");

    private enum Format {
        JPEG("jpg", "jpeg"),
        PNG("png", "png");

        private final String extension;
        private final String imageIoName;

        Format(String extension, String imageIoName) {
            this.extension = extension;
            this.imageIoName = imageIoName;
        }
    }

    private final Path directory;
    private final String publicBaseUrl;

    public PhotoStorageService(@Value("${app.upload.dir}") String uploadDir,
                               @Value("${app.upload.public-base-url}") String publicBaseUrl) {
        this.directory = Path.of(uploadDir).toAbsolutePath().normalize().resolve("photos");
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    /**
     * Devolve o photoUrl que deve ser gravado no registro: o atual, se nenhuma foto nova foi enviada,
     * ou o endereço da foto nova. A foto antiga é apagada quando a transação confirma;
     * se a transação falhar, a foto nova é que é apagada.
     */
    public String replace(MultipartFile newPhoto, String currentUrl) {
        if (newPhoto == null || newPhoto.isEmpty()) {
            return currentUrl;
        }
        String newUrl = store(newPhoto);
        afterTransaction(currentUrl, newUrl);
        return newUrl;
    }

    /**
     * Apaga a foto de um registro excluído, depois que a exclusão for confirmada.
     */
    public void release(String photoUrl) {
        afterTransaction(photoUrl, null);
    }

    /**
     * Procura o arquivo de uma foto salva por este sistema; nomes fora do padrão nunca são aceitos.
     */
    public Optional<Path> find(String fileName) {
        if (fileName == null || !STORED_NAME.matcher(fileName).matches()) {
            return Optional.empty();
        }
        Path file = directory.resolve(fileName).normalize();
        return file.startsWith(directory) && Files.isRegularFile(file) ? Optional.of(file) : Optional.empty();
    }

    private String store(MultipartFile photo) {
        byte[] original = readBytes(photo);
        Format format = detectFormat(original);
        byte[] clean = reencode(original, format);
        String fileName = UUID.randomUUID() + "." + format.extension;
        Path target = directory.resolve(fileName).normalize();
        if (!target.startsWith(directory)) {
            throw new PhotoException("Não foi possível salvar a foto.");
        }
        try {
            Files.createDirectories(directory);
            Files.write(target, clean, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            log.error("Falha ao gravar a foto em {}", target, e);
            throw new PhotoException("Não foi possível salvar a foto. Tente novamente.");
        }
        return publicBaseUrl + URL_PREFIX + fileName;
    }

    private byte[] readBytes(MultipartFile photo) {
        if (photo.getSize() > MAX_BYTES) {
            throw new PhotoException("A foto deve ter no máximo 2 MB.");
        }
        try {
            return photo.getBytes();
        } catch (IOException e) {
            log.error("Falha ao ler a foto enviada", e);
            throw new PhotoException("Não foi possível ler a foto enviada.");
        }
    }

    /**
     * Identifica o formato pelos primeiros bytes do arquivo; extensão e tipo informados pelo navegador não contam.
     */
    private Format detectFormat(byte[] bytes) {
        if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return Format.JPEG;
        }
        byte[] pngSignature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length > pngSignature.length) {
            boolean png = true;
            for (int i = 0; i < pngSignature.length; i++) {
                png &= bytes[i] == pngSignature[i];
            }
            if (png) {
                return Format.PNG;
            }
        }
        throw new PhotoException("Formato de imagem não aceito. Envie uma foto JPEG ou PNG.");
    }

    /**
     * Decodifica a imagem (conferindo as dimensões antes) e grava de novo, para o arquivo salvo ser gerado pelo servidor.
     */
    private byte[] reencode(byte[] bytes, Format format) {
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format.imageIoName);
            if (!readers.hasNext()) {
                throw new PhotoException("Não foi possível processar a foto.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_SIDE || height > MAX_SIDE
                        || (long) width * height > MAX_PIXELS) {
                    throw new PhotoException("A imagem é grande demais (máximo de 4096 x 4096 pixels).");
                }
                BufferedImage image = reader.read(0);
                BufferedImage output = format == Format.JPEG ? withoutTransparency(image) : image;
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                if (!ImageIO.write(output, format.imageIoName, result)) {
                    throw new PhotoException("Não foi possível processar a foto.");
                }
                return result.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (PhotoException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            log.warn("Imagem rejeitada: {}", e.getMessage());
            throw new PhotoException("A imagem está corrompida ou não é válida.");
        }
    }

    private BufferedImage withoutTransparency(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    /**
     * Quando a transação termina: se confirmou, apaga "deleteOnCommit"; se desfez, apaga "deleteOnRollback".
     */
    private void afterTransaction(String deleteOnCommit, String deleteOnRollback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete(deleteOnCommit);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                delete(status == STATUS_COMMITTED ? deleteOnCommit : deleteOnRollback);
            }
        });
    }

    /**
     * Só apaga arquivos que este sistema criou; URLs externas e valores estranhos são ignorados.
     */
    private void delete(String photoUrl) {
        Optional<String> fileName = ownFileName(photoUrl);
        if (fileName.isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(directory.resolve(fileName.get()));
        } catch (IOException e) {
            log.warn("Não foi possível apagar a foto {}", fileName.get(), e);
        }
    }

    private Optional<String> ownFileName(String photoUrl) {
        if (photoUrl == null) {
            return Optional.empty();
        }
        String path = photoUrl.startsWith(publicBaseUrl) ? photoUrl.substring(publicBaseUrl.length()) : photoUrl;
        if (!path.startsWith(URL_PREFIX)) {
            return Optional.empty();
        }
        String fileName = path.substring(URL_PREFIX.length());
        return STORED_NAME.matcher(fileName).matches() ? Optional.of(fileName) : Optional.empty();
    }
}
