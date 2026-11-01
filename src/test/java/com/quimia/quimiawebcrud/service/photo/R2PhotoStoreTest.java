package com.quimia.quimiawebcrud.service.photo;

import com.quimia.quimiawebcrud.exception.PhotoException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * As fotos no R2: o app confere e regrava a imagem como sempre e só então a envia ao bucket, guarda no banco o
 * endereço público do bucket e apaga só o que é dele. O cliente S3 é de mentira: o teste não usa rede nem conta.
 */
class R2PhotoStoreTest {

    private static final String BASE = "https://fotos.quimia.tech";
    private static final String NAME = "0f8fad5b-d9cb-469f-a165-70867728950e.png";

    private final S3Client s3 = mock(S3Client.class);

    private R2PhotoStore store() {
        return new R2PhotoStore(s3, "quimia-fotos", BASE + "/");
    }

    private static byte[] png() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    @Test
    void savingSendsTheBytesToTheBucketAndReturnsThePublicAddress() {
        String url = store().save(NAME, new byte[]{1, 2, 3}, "image/png");

        assertEquals(BASE + "/" + NAME, url, "o endereço do banco é o público do bucket, sem barra repetida");
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertEquals("quimia-fotos", request.getValue().bucket());
        assertEquals(NAME, request.getValue().key());
        assertEquals("image/png", request.getValue().contentType());
        assertTrue(request.getValue().cacheControl().contains("immutable"), "o navegador pode guardar a foto por muito tempo");
    }

    @Test
    void deletingAsksTheBucketToRemoveThatFile() {
        store().delete(NAME);

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(request.capture());
        assertEquals("quimia-fotos", request.getValue().bucket());
        assertEquals(NAME, request.getValue().key());
    }

    @Test
    void onlyAddressesOfThisBucketAreRecognisedAsItsOwn() {
        R2PhotoStore store = store();

        assertEquals(NAME, store.ownFileName(BASE + "/" + NAME).orElseThrow());
        assertFalse(store.ownFileName("https://outro-site.com/" + NAME).isPresent(), "foto de outro site");
        assertFalse(store.ownFileName(BASE + "/../" + NAME).isPresent(), "tentativa de sair da pasta");
        assertFalse(store.ownFileName(BASE + "/foto-qualquer.png").isPresent(), "nome fora do padrão que o sistema gera");
        assertFalse(store.ownFileName("http://localhost:8080/uploads/photos/" + NAME).isPresent(), "foto antiga em disco");
        assertFalse(store.ownFileName(null).isPresent());
    }

    @Test
    void theR2DoesNotServePhotosThroughTheApp() {
        assertFalse(store().find(NAME).isPresent(), "a rota /uploads/photos só serve o que está em disco");
    }

    @Test
    void withoutABucketOrAPublicAddressTheAppDoesNotStart() {
        assertThrows(IllegalStateException.class, () -> new R2PhotoStore(s3, "", BASE));
        assertThrows(IllegalStateException.class, () -> new R2PhotoStore(s3, "quimia-fotos", " "));
    }

    @Test
    void thePhotoServiceChecksAndRewritesTheImageBeforeSendingItToTheBucket() throws Exception {
        PhotoStorageService service = new PhotoStorageService(store());

        String url = service.replace(new MockMultipartFile("photo", "a.png", "image/png", png()), null);

        assertTrue(url.startsWith(BASE + "/") && url.endsWith(".png"), url);
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertEquals("image/png", request.getValue().contentType(), "o tipo vem do conteúdo, não do que o navegador disse");
    }

    @Test
    void aFileThatIsNotAnImageNeverReachesTheBucket() {
        PhotoStorageService service = new PhotoStorageService(store());
        MockMultipartFile text = new MockMultipartFile("photo", "a.png", "image/png", "isto não é uma imagem".getBytes());

        assertThrows(PhotoException.class, () -> service.replace(text, null));

        verifyNoInteractions(s3);
    }

    @Test
    void whenTheBucketFailsTheUserSeesAFriendlyMessage() throws Exception {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("sem conexão"));
        PhotoStorageService service = new PhotoStorageService(store());
        MockMultipartFile photo = new MockMultipartFile("photo", "a.png", "image/png", png());

        PhotoException error = assertThrows(PhotoException.class, () -> service.replace(photo, null));

        assertEquals("Não foi possível salvar a foto. Tente novamente.", error.getMessage());
    }

    @Test
    void releasingDeletesOnlyThePhotosThatBelongToTheBucket() {
        PhotoStorageService service = new PhotoStorageService(store());

        service.release("https://outro-site.com/" + NAME);
        verifyNoInteractions(s3);

        service.release(BASE + "/" + NAME);
        verify(s3).deleteObject(any(DeleteObjectRequest.class));
    }
}
