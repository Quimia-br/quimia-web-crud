package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Entrega as fotos salvas. A rota é pública (o aplicativo também lê as fotos), mas só devolve arquivos
 * criados pelo sistema, sempre como imagem e com cabeçalhos que impedem o navegador de executar o conteúdo.
 */
@RestController
@RequestMapping(PhotoStorageService.URL_PREFIX)
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoStorageService photoStorage;

    /** Devolve a foto pelo nome do arquivo; 404 se não for um arquivo criado pelo sistema. */
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> photo(@PathVariable String fileName) {
        return photoStorage.find(fileName)
                .map(this::imageResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<Resource> imageResponse(Path file) {
        MediaType type = file.getFileName().toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(new FileSystemResource(file));
    }
}
