package com.quimia.quimiawebcrud.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O filtro do segredo de origem: só passa o pedido que traz o cabeçalho certo; o /health fica de fora; sem segredo
 * configurado ele não faz nada.
 */
class OriginSecretFilterTest {

    private static final String SECRET = "um-segredo-bem-longo-e-aleatorio";

    /** O que aconteceu com um pedido: o código da resposta, o corpo e se ele chegou ao resto do app. */
    private record Outcome(int status, String body, boolean reachedTheApp, String secretHeader) {
    }

    private Outcome call(OriginSecretFilter filter, String contextPath, String uri, String sentSecret)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", contextPath + uri);
        request.setContextPath(contextPath);
        if (sentSecret != null) {
            request.addHeader(OriginSecretFilter.HEADER, sentSecret);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return new Outcome(response.getStatus(), response.getContentAsString(), chain.getRequest() != null,
                response.getHeader(OriginSecretFilter.HEADER));
    }

    @Test
    void aRequestWithTheRightSecretPasses() throws Exception {
        Outcome outcome = call(new OriginSecretFilter(SECRET), "/admin", "/users", SECRET);

        assertEquals(200, outcome.status());
        assertTrue(outcome.reachedTheApp());
    }

    @Test
    void aRequestWithoutTheSecretIsRefusedWith403AndNeverReachesTheApp() throws Exception {
        Outcome outcome = call(new OriginSecretFilter(SECRET), "/admin", "/login", null);

        assertEquals(403, outcome.status());
        assertFalse(outcome.reachedTheApp());
        assertEquals("", outcome.body(), "a recusa não explica nada a quem tenta");
    }

    @Test
    void aWrongSecretIsRefused() throws Exception {
        assertEquals(403, call(new OriginSecretFilter(SECRET), "/admin", "/users", "outro-segredo").status());
    }

    @Test
    void aPrefixOrAnExtendedVersionOfTheSecretIsNotAccepted() throws Exception {
        OriginSecretFilter filter = new OriginSecretFilter(SECRET);

        assertEquals(403, call(filter, "/admin", "/users", SECRET.substring(0, SECRET.length() - 1)).status());
        assertEquals(403, call(filter, "/admin", "/users", SECRET + "x").status());
        assertEquals(403, call(filter, "/admin", "/users", "").status());
    }

    @Test
    void theHealthCheckPassesWithoutTheSecretWithAndWithoutThePrefix() throws Exception {
        OriginSecretFilter filter = new OriginSecretFilter(SECRET);

        assertTrue(call(filter, "/admin", "/health", null).reachedTheApp());
        assertTrue(call(filter, "", "/health", null).reachedTheApp());
    }

    @Test
    void onlyTheExactHealthPathIsExempt() throws Exception {
        OriginSecretFilter filter = new OriginSecretFilter(SECRET);

        assertEquals(403, call(filter, "/admin", "/health/x", null).status());
        assertEquals(403, call(filter, "/admin", "/healthz", null).status());
        assertEquals(403, call(filter, "", "/admin/health", null).status(), "o prefixo faz parte do app, não do caminho");
    }

    @Test
    void withoutAConfiguredSecretTheFilterDoesNothing() throws Exception {
        assertTrue(call(new OriginSecretFilter(""), "", "/users", null).reachedTheApp());
        assertTrue(call(new OriginSecretFilter(null), "", "/users", null).reachedTheApp());
    }

    @Test
    void theFilterNeverLeaksTheSecretInTheResponse() throws Exception {
        Outcome outcome = call(new OriginSecretFilter(SECRET), "/admin", "/users", "errado");

        assertNull(outcome.secretHeader());
        assertFalse(outcome.body().contains(SECRET));
    }
}
