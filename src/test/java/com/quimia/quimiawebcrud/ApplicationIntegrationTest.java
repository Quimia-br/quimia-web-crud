package com.quimia.quimiawebcrud;

import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.*;
import com.quimia.quimiawebcrud.security.PasswordHasher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "app.upload.dir=target/it-uploads",
        "app.upload.public-base-url=http://app.test"})
class ApplicationIntegrationTest {
    /** Transaction manager that never touches a database but keeps Spring's transaction synchronization (used for photo cleanup). */
    @TestConfiguration
    static class NoDbTransactions {
        @Bean
        AbstractPlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override protected Object doGetTransaction() { return new Object(); }
                @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
                @Override protected void doCommit(DefaultTransactionStatus status) { }
                @Override protected void doRollback(DefaultTransactionStatus status) { }
            };
        }
    }

    @AfterAll
    static void cleanUploads() throws Exception {
        Path root = Path.of("target/it-uploads");
        if (Files.exists(root)) {
            try (Stream<Path> files = Files.walk(root)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @LocalServerPort int port;
    @Autowired PasswordHasher hasher;
    @MockitoBean AdminRepository admins;
    @MockitoBean UserRepository users;
    @MockitoBean CompanyRepository companies;
    @MockitoBean ProductRepository products;
    @MockitoBean DisposalRepository disposals;
    @MockitoBean ShelfRepository shelves;
    @MockitoBean HistoryRepository histories;
    @MockitoBean HistoryTypeRepository historyTypes;
    @MockitoBean LocationRepository locations;
    @MockitoBean AdminLogRepository adminLogs;
    @MockitoBean HistoryProductRepository historyProducts;
    @MockitoBean ShelfProductRepository shelfProducts;
    @MockitoBean UserProductRepository userProducts;

    final List<User> userStore = new ArrayList<>();

    String base() { return "http://localhost:" + port; }

    HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    HttpResponse<String> get(HttpClient c, String path) throws Exception {
        return c.send(HttpRequest.newBuilder(URI.create(base() + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> postForm(HttpClient c, String path, String body) throws Exception {
        return c.send(HttpRequest.newBuilder(URI.create(base() + path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    static String csrf(String html) {
        Matcher m = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(m.find(), "csrf token present in page");
        return m.group(1);
    }

    HttpResponse<String> multipart(HttpClient c, String path, Map<String, String> fields, String fileName, String fileType, byte[] file) throws Exception {
        String boundary = "----tmpboundary" + System.nanoTime();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (var e : fields.entrySet()) {
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n" + e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        if (file != null) {
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"" + fileName + "\"\r\nContent-Type: " + fileType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(file);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return c.send(HttpRequest.newBuilder(URI.create(base() + path)).header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    }

    static byte[] png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    @Test void loginUploadAndLogoutWorkOnARealServer() throws Exception {
        when(admins.findByEmailIgnoreCase(anyString())).thenAnswer(i ->
                "adm@quimia.com".equalsIgnoreCase(i.getArgument(0))
                        ? Optional.of(Admin.builder().id(1).userName("Adm").email("adm@quimia.com").status(Status.ATIVO).passwordHash(hasher.hash("Segredo#1")).build())
                        : Optional.empty());
        userStore.add(User.builder().id(1).userName("Lucas Almeida").email("l@x.com").status(Status.ATIVO).signUpDate(LocalDateTime.of(2026, 9, 5, 10, 0)).build());
        when(users.findAll()).thenAnswer(i -> new ArrayList<>(userStore));
        when(users.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            if (u.getId() == null) {
                u = User.builder().id(userStore.size() + 1).userName(u.getUserName()).email(u.getEmail()).status(u.getStatus())
                        .photoUrl(u.getPhotoUrl()).passwordHash(u.getPasswordHash()).build();
                userStore.add(u);
            }
            return u;
        });

        HttpClient c = client();
        // 1. protected page redirects to login
        HttpResponse<String> r = get(c, "/users");
        assertEquals(302, r.statusCode());
        assertTrue(r.headers().firstValue("Location").orElse("").endsWith("/login"), r.headers().firstValue("Location").orElse(""));

        // 2. login page + CSRF token, wrong password first
        String loginPage = get(c, "/login").body();
        String token = csrf(loginPage);
        assertTrue(loginPage.contains("password-toggle"), "eye on the login page");
        assertFalse(loginPage.contains("Cadastre-se"), "no self-registration link");
        r = postForm(c, "/login", "email=adm%40quimia.com&password=errada&_csrf=" + token);
        assertEquals(302, r.statusCode());
        assertTrue(r.headers().firstValue("Location").get().endsWith("/login?error"));
        assertEquals(403, postForm(c, "/login", "email=adm%40quimia.com&password=Segredo%231").statusCode(), "no csrf -> 403");

        // 3. correct login goes back to the page originally requested
        token = csrf(get(c, "/login").body());
        r = postForm(c, "/login", "email=adm%40quimia.com&password=Segredo%231&_csrf=" + token);
        assertEquals(302, r.statusCode());
        String landing = r.headers().firstValue("Location").get();
        assertTrue(landing.contains("/users"), landing);
        assertEquals(200, get(c, "/").statusCode(), "dashboard opens");

        // 4. list page with real data, real CSRF tokens in the popups, security headers, filters, pagination markup
        r = get(c, "/users");
        assertEquals(200, r.statusCode());
        String html = r.body();
        assertTrue(html.contains("Lucas Almeida") && html.contains("editar-user-1"));
        assertTrue(html.contains("enctype=\"multipart/form-data\"") && html.contains("name=\"photo\""));
        assertTrue(html.contains("password-toggle") && html.contains("sidebar-logout-form"));
        assertTrue(r.headers().firstValue("Content-Security-Policy").orElse("").contains("script-src 'self'"));
        assertEquals(200, get(c, "/users?busca=lucas&status=ATIVO&page=1").statusCode());
        assertFalse(get(c, "/users?busca=zzz").body().contains("Lucas Almeida"), "filter hides non-matching rows");
        for (String p : new String[]{"/admins", "/companies", "/products", "/disposals", "/shelves", "/histories", "/history-types", "/locations", "/admin-logs"})
            assertEquals(200, get(c, p).statusCode(), p);
        token = csrf(html);

        // 5. create user with photo (multipart + CSRF inside the body, on a real Tomcat)
        Map<String, String> fields = Map.of("userName", "Ana Nova", "email", "ana@x.com", "password", "Outra#Senha1", "status", "ATIVO", "_csrf", token, "photoUrl", "https://forjado.com/x.png");
        r = multipart(c, "/users/save", fields, "minha foto.html", "text/html", png());
        assertEquals(302, r.statusCode(), r.body());
        assertTrue(r.headers().firstValue("Location").get().endsWith("/users"));
        assertEquals(2, userStore.size(), "user created");
        assertTrue(get(c, "/users").body().contains("Registro criado com sucesso."), "success message shown");
        User created = userStore.get(1);
        assertTrue(created.getPhotoUrl().matches("http://app\\.test/uploads/photos/[0-9a-f-]{36}\\.png"), created.getPhotoUrl());
        assertTrue(hasher.matches("Outra#Senha1", created.getPasswordHash()), "password stored as argon2 hash");
        assertTrue(created.getPasswordHash().startsWith("$argon2id$"));
        String fileName = created.getPhotoUrl().substring(created.getPhotoUrl().lastIndexOf('/') + 1);
        assertTrue(Files.exists(Path.of("target/it-uploads", "photos", fileName)), "file on disk");

        // 6. the photo is public (no cookies) and served as a safe image
        HttpResponse<byte[]> photo = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base() + "/uploads/photos/" + fileName)).build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, photo.statusCode());
        assertEquals("image/png", photo.headers().firstValue("Content-Type").get());
        assertEquals("nosniff", photo.headers().firstValue("X-Content-Type-Options").get());
        assertTrue(photo.headers().firstValue("Content-Security-Policy").get().contains("sandbox"));
        assertTrue(get(c, "/users").body().contains("src=\"http://app.test/uploads/photos/" + fileName + "\""), "preview of the current photo");

        // 7. attacks: no csrf, disguised file, traversal
        assertEquals(403, multipart(c, "/users/save", Map.of("userName", "X", "email", "x@x.com", "password", "p", "status", "ATIVO"), "a.png", "image/png", png()).statusCode(), "multipart without csrf -> 403");
        int before = userStore.size();
        Map<String, String> evil = Map.of("userName", "Evil", "email", "e@x.com", "password", "SenhaForte1", "status", "ATIVO", "_csrf", csrf(get(c, "/users").body()));
        r = multipart(c, "/users/save", evil, "x.jpg", "image/jpeg", "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));
        assertEquals(302, r.statusCode());
        assertEquals(before, userStore.size(), "disguised file rejected, nothing saved");
        assertTrue(get(c, "/users").body().contains("Formato de imagem não aceito"), "friendly error shown once");
        int traversal = get(client(), "/uploads/photos/..%2F..%2Fapplication.properties").statusCode();
        assertTrue(traversal >= 400 && traversal < 500, "traversal refused: " + traversal);
        try (Stream<Path> files = Files.list(Path.of("target/it-uploads", "photos"))) {
            assertEquals(1, files.count(), "only the valid photo is on disk");
        }

        // 8. logout needs POST + CSRF
        token = csrf(get(c, "/users").body());
        get(c, "/logout");
        assertEquals(200, get(c, "/users").statusCode(), "GET /logout does not log out");
        r = postForm(c, "/logout", "_csrf=" + token);
        assertEquals(302, r.statusCode());
        assertTrue(r.headers().firstValue("Location").get().endsWith("/login?logout"));
        assertEquals(302, get(c, "/users").statusCode(), "after logout the panel is closed again");
    }
}
