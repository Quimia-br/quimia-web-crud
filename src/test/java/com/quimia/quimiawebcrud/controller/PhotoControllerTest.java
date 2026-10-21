package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.PhotoStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A rota pública das fotos só entrega arquivos criados pelo sistema, sempre como imagem e com cabeçalhos seguros.
 */
@WebMvcTest(PhotoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PhotoStorageService.class)
@TestPropertySource(properties = {"app.upload.dir=target/test-uploads", "app.upload.public-base-url=http://app.test"})
class PhotoControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    PhotoStorageService storage;

    @Test
    void servesAStoredPhotoWithSafeHeadersAndRefusesEverythingElse() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", png);
        String url = storage.replace(new MockMultipartFile("photo", "a.png", "image/png", png.toByteArray()), null);
        String name = url.substring(url.lastIndexOf('/') + 1);
        try {
            mvc.perform(get("/uploads/photos/" + name))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/png"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("Content-Security-Policy", "default-src 'none'; sandbox"));

            mvc.perform(get("/uploads/photos/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
            mvc.perform(get("/uploads/photos/" + name.replace(".png", ".svg"))).andExpect(status().isNotFound());
            mvc.perform(get("/uploads/photos/..%2F..%2Fapplication.properties")).andExpect(status().is4xxClientError());
        } finally {
            storage.release(url);
        }
    }
}
