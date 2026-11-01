package com.quimia.quimiawebcrud;

import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import com.quimia.quimiawebcrud.repository.DisposalRepository;
import com.quimia.quimiawebcrud.repository.HistoryProductRepository;
import com.quimia.quimiawebcrud.repository.HistoryRepository;
import com.quimia.quimiawebcrud.repository.HistoryTypeRepository;
import com.quimia.quimiawebcrud.repository.LocationRepository;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import com.quimia.quimiawebcrud.repository.ShelfProductRepository;
import com.quimia.quimiawebcrud.repository.ShelfRepository;
import com.quimia.quimiawebcrud.repository.UserProductRepository;
import com.quimia.quimiawebcrud.repository.UserRepository;
import com.quimia.quimiawebcrud.repository.support.DashboardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O painel em produção fica em quimia.tech/admin, atrás de proxies (Cloudflare e Render). Aqui o app sobe inteiro com
 * o prefixo /admin e confere que tudo vive sob ele e que os cabeçalhos do proxy são respeitados.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.servlet.context-path=/admin",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "app.upload.dir=target/it-uploads-prefix",
        "app.upload.public-base-url=http://app.test/admin"})
class AdminPrefixIntegrationTest {

    /** Gerenciador de transação que não toca em banco nenhum (o teste não tem banco). */
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

    @LocalServerPort
    int port;

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
    @MockitoBean DashboardRepository dashboard;

    private HttpResponse<String> get(String path, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void theLoginPageLivesUnderThePrefixAndItsFormPostsThere() throws Exception {
        HttpResponse<String> response = get("/admin/login");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("action=\"/admin/login\""), "o formulário envia para /admin/login");
        assertTrue(response.body().contains("/admin/css/"), "os arquivos visuais também ficam sob /admin");
    }

    @Test
    void aProtectedPageSendsTheVisitorToTheLoginUnderThePrefix() throws Exception {
        HttpResponse<String> response = get("/admin/users");

        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("Location").orElse("").endsWith("/admin/login"));
    }

    @Test
    void withoutThePrefixNothingIsServed() throws Exception {
        assertEquals(404, get("/login").statusCode());
    }

    @Test
    void theProxyHeadersDecideTheHostAndTheProtocolOfTheRedirect() throws Exception {
        HttpResponse<String> response = get("/admin/users", "X-Forwarded-Host", "quimia.tech", "X-Forwarded-Proto", "https");

        assertEquals("https://quimia.tech/admin/login", response.headers().firstValue("Location").orElse(""));
    }
}
