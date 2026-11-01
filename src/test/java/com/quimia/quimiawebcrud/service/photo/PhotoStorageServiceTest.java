package com.quimia.quimiawebcrud.service.photo;

import com.quimia.quimiawebcrud.dto.request.UserRequestDTO;
import com.quimia.quimiawebcrud.exception.PhotoException;
import com.quimia.quimiawebcrud.mapper.UserMapper;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.UserRepository;
import com.quimia.quimiawebcrud.security.PasswordHasher;
import com.quimia.quimiawebcrud.service.EditLog;
import com.quimia.quimiawebcrud.service.crud.UserService;
import com.quimia.quimiawebcrud.service.removal.RecordRemovalService;
import com.quimia.quimiawebcrud.service.verification.ExternalChecks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validação e armazenamento das fotos (usuários e empresas): só imagem real, regravada pelo servidor, com nome aleatório.
 */
class PhotoStorageServiceTest {

    @TempDir
    Path dir;

    private PhotoStorageService storage() {
        return new PhotoStorageService(new DiskPhotoStore(dir.toString(), "http://app.test"));
    }

    private static byte[] image(String format, int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("photo", name, "image/jpeg", bytes);
    }

    private long storedFiles() throws IOException {
        Path photos = dir.resolve("photos");
        if (Files.notExists(photos)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(photos)) {
            return files.count();
        }
    }

    private void assertRejected(String why, byte[] bytes) throws IOException {
        assertThrows(PhotoException.class, () -> storage().replace(file("x.jpg", bytes), null), why);
        assertEquals(0, storedFiles(), "nothing saved when rejected: " + why);
    }

    @Test
    void acceptsRealImagesRewritesThemAndIgnoresTheSentName() throws Exception {
        for (String format : new String[]{"png", "jpeg"}) {
            byte[] original = image(format, 20, 10);
            byte[] script = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
            byte[] withPayload = Arrays.copyOf(original, original.length + script.length);
            System.arraycopy(script, 0, withPayload, original.length, script.length);

            String url = storage().replace(file("minha foto.html", withPayload), null);

            assertTrue(url.matches("http://app\\.test/uploads/photos/[0-9a-f-]{36}\\.(png|jpg)"), url);
            Path saved = dir.resolve("photos").resolve(url.substring(url.lastIndexOf('/') + 1));
            assertFalse(new String(Files.readAllBytes(saved), StandardCharsets.ISO_8859_1).contains("<script>"),
                    "o conteúdo escondido não sobrevive à regravação");
            assertNotNull(ImageIO.read(saved.toFile()), "o arquivo salvo é uma imagem de verdade");
        }
    }

    @Test
    void rejectsDisguisedAndInvalidFiles() throws Exception {
        assertRejected("html", "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));
        assertRejected("svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8));
        assertRejected("gif", "GIF89a......".getBytes(StandardCharsets.UTF_8));
        assertRejected("php", "<?php system($_GET['c']); ?>".getBytes(StandardCharsets.UTF_8));

        byte[] fakeJpeg = new byte[500];
        new Random(1).nextBytes(fakeJpeg);
        fakeJpeg[0] = (byte) 0xFF;
        fakeJpeg[1] = (byte) 0xD8;
        fakeJpeg[2] = (byte) 0xFF;
        assertRejected("jpeg header with garbage inside", fakeJpeg);

        byte[] png = image("png", 50, 50);
        assertRejected("truncated png", Arrays.copyOf(png, png.length / 2));

        byte[] tooBig = new byte[2 * 1024 * 1024 + 1];
        tooBig[0] = (byte) 0xFF;
        tooBig[1] = (byte) 0xD8;
        tooBig[2] = (byte) 0xFF;
        assertRejected("over 2 MB", tooBig);
        assertRejected("over 4096 pixels", image("png", 5000, 5000));
    }

    @Test
    void withoutANewFileTheCurrentPhotoStays() throws Exception {
        assertEquals("keep-me", storage().replace(null, "keep-me"));
        assertEquals("keep-me", storage().replace(new MockMultipartFile("photo", new byte[0]), "keep-me"));
        assertNull(storage().replace(null, null));
    }

    @Test
    void replacingDeletesOnlyThePhotosCreatedByTheSystem() throws Exception {
        PhotoStorageService storage = storage();
        String first = storage.replace(file("a.png", image("png", 8, 8)), null);
        Path firstFile = dir.resolve("photos").resolve(first.substring(first.lastIndexOf('/') + 1));

        String second = storage.replace(file("b.png", image("png", 8, 8)), first);

        assertNotEquals(first, second);
        assertFalse(Files.exists(firstFile), "the old photo is removed");
        storage.replace(file("c.png", image("png", 8, 8)), "https://cdn.exemplo.com/foto.jpg");   // external url: nothing to delete
        storage.release("../../../etc/passwd");                                                   // never touches anything else
        storage.release(second);
        assertEquals(1, storedFiles(), "only the third photo is left");
    }

    @Test
    void findRefusesAnythingThatIsNotAStoredPhotoName() throws Exception {
        PhotoStorageService storage = storage();
        String url = storage.replace(file("a.png", image("png", 8, 8)), null);
        String name = url.substring(url.lastIndexOf('/') + 1);

        assertTrue(storage.find(name).isPresent());
        for (String bad : new String[]{"../" + name, "..\\" + name, name.toUpperCase(), "x.png", name + ".exe",
                "../../etc/passwd", "", name.replace(".png", ".svg")}) {
            assertTrue(storage.find(bad).isEmpty(), bad);
        }
        assertTrue(storage.find(null).isEmpty());
    }

    @Test
    void urlPolicyOnlyAllowsHttpAddressesAndStoredPhotos() {
        PhotoUrlPolicy policy = new PhotoUrlPolicy();

        assertTrue(policy.isSafe("https://i.pravatar.cc/150?img=3"));
        assertTrue(policy.isSafe("http://x.com/a.jpg"));
        assertTrue(policy.isSafe("/uploads/photos/8f3a0c1e-1111-2222-3333-444455556666.png"));
        for (String bad : new String[]{null, "", "javascript:alert(1)", "JaVaScRiPt:alert(1)", "data:text/html;base64,AAA",
                "//evil.com/x.png", "https://x.com/a\"onerror=\"alert(1)", "https://x.com/a b", "https://x.com/<script>",
                "/uploads/photos/../../x.png", "ftp://x/a.png", "/etc/passwd"}) {
            assertFalse(policy.isSafe(bad), String.valueOf(bad));
        }
    }

    @Test
    void userServiceIgnoresAForgedPhotoUrlAndCleansUpTheFiles() throws Exception {
        PhotoStorageService storage = storage();
        UserRepository repository = mock(UserRepository.class);
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserService service = new UserService(repository, new UserMapper(), new PasswordHasher(), storage, mock(RecordRemovalService.class),
                mock(EditLog.class), mock(ExternalChecks.class));

        UserRequestDTO forged = UserRequestDTO.builder().userName("Ana").email("a@x.com").password("s3nha-forte")
                .status(Status.ATIVO).photoUrl("https://forjado.com/x.png").build();
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        service.create(forged);
        verify(repository).save(saved.capture());
        assertNull(saved.getValue().getPhotoUrl(), "o texto enviado pelo formulário não vira photoUrl");

        String own = storage.replace(file("a.png", image("png", 8, 8)), null);
        User existing = User.builder().id(1).userName("Ana").photoUrl(own).build();
        when(repository.findById(1)).thenReturn(Optional.of(existing));

        service.update(1, UserRequestDTO.builder().userName("Ana 2").photoUrl("https://forjado.com/y.png").build());
        assertEquals(own, existing.getPhotoUrl(), "sem arquivo novo a foto atual é mantida");

        service.update(1, UserRequestDTO.builder().userName("Ana 3").photo(file("n.png", image("png", 8, 8))).build());
        assertNotEquals(own, existing.getPhotoUrl());
        assertFalse(Files.exists(dir.resolve("photos").resolve(own.substring(own.lastIndexOf('/') + 1))), "foto antiga removida");

        String current = existing.getPhotoUrl();
        service.delete(1);
        assertFalse(Files.exists(dir.resolve("photos").resolve(current.substring(current.lastIndexOf('/') + 1))),
                "a foto sai junto com o usuário");
    }
}
