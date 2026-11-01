package com.quimia.quimiawebcrud.service.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Cliente do R2, criado só quando as fotos vão para ele. As chaves vêm de variáveis de ambiente
 * (R2_ENDPOINT, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_REGION) e nunca ficam no código.
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "r2")
public class R2Config {

    @Bean
    S3Client r2Client(@Value("${app.r2.endpoint}") String endpoint,
                      @Value("${app.r2.access-key-id}") String accessKeyId,
                      @Value("${app.r2.secret-access-key}") String secretAccessKey,
                      @Value("${app.r2.region:auto}") String region) {
        if (endpoint.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank()) {
            throw new IllegalStateException(
                    "Com app.storage.type=r2, informe R2_ENDPOINT, R2_ACCESS_KEY_ID e R2_SECRET_ACCESS_KEY.");
        }
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                // O R2 não tem região de verdade: "auto" é o valor que ele espera
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                // O SDK novo manda uma soma de verificação extra que o R2 não aceita; só calcula quando é exigida
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }
}
