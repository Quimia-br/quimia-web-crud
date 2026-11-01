package com.quimia.quimiawebcrud.service.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;

/**
 * Guarda as fotos num bucket da Cloudflare R2 (compatível com S3). O endereço gravado no banco já é o público do
 * bucket (ex.: https://fotos.quimia.tech/NOME.jpg), então o navegador e o aplicativo buscam a foto direto lá, sem
 * passar por este app. Serve para hospedagem que apaga o disco a cada reinício.
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "r2")
public class R2PhotoStore implements PhotoStore {

    /** O nome da foto é único e o conteúdo nunca muda, então o navegador pode guardá-la por um ano. */
    private static final String CACHE = "public, max-age=31536000, immutable";

    private final S3Client client;
    private final String bucket;
    private final String publicBaseUrl;

    public R2PhotoStore(S3Client client,
                        @Value("${app.r2.bucket}") String bucket,
                        @Value("${app.r2.public-base-url}") String publicBaseUrl) {
        if (bucket.isBlank() || publicBaseUrl.isBlank()) {
            throw new IllegalStateException("Com app.storage.type=r2, informe R2_BUCKET e R2_PUBLIC_BASE_URL.");
        }
        this.client = client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    public String save(String fileName, byte[] content, String contentType) {
        client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileName)
                        .contentType(contentType)
                        .cacheControl(CACHE)
                        .build(),
                RequestBody.fromBytes(content));
        return publicBaseUrl + "/" + fileName;
    }

    @Override
    public void delete(String fileName) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(fileName).build());
    }

    @Override
    public Optional<String> ownFileName(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith(publicBaseUrl + "/")) {
            return Optional.empty();
        }
        String fileName = photoUrl.substring(publicBaseUrl.length() + 1);
        return STORED_NAME.matcher(fileName).matches() ? Optional.of(fileName) : Optional.empty();
    }
}
