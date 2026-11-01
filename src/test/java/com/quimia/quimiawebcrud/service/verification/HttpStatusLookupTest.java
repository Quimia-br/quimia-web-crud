package com.quimia.quimiawebcrud.service.verification;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A consulta por código de resposta, contra um servidor de mentira na própria máquina (nenhum teste vai à internet).
 */
class HttpStatusLookupTest {

    private HttpServer server;
    private final List<String> paths = new CopyOnWriteArrayList<>();
    private final List<String> userAgents = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile long delayMillis = 0;

    @BeforeEach
    void startFakeSource() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopFakeSource() {
        server.stop(0);
    }

    private HttpStatusLookup lookup(Duration timeout) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
        return new HttpStatusLookup(client, "http://127.0.0.1:" + server.getAddress().getPort() + "/api/%s", timeout, "fonte de teste");
    }

    @Test
    void status200MeansFound() {
        status = 200;

        assertEquals(LookupResult.FOUND, lookup(Duration.ofSeconds(3)).check("11222333000181"));
        assertEquals(List.of("/api/11222333000181"), paths);
    }

    @Test
    void status404MeansNotFound() {
        status = 404;

        assertEquals(LookupResult.NOT_FOUND, lookup(Duration.ofSeconds(3)).check("98765432000198"));
    }

    @Test
    void anyOtherStatusMeansUnknownSoTheRegistrationIsNotBlocked() {
        for (int other : new int[]{400, 403, 429, 500, 503}) {
            status = other;
            assertEquals(LookupResult.UNKNOWN, lookup(Duration.ofSeconds(3)).check("11222333000181"), "status " + other);
        }
    }

    @Test
    void aSlowSourceIsGivenUpOnAtTheTimeout() {
        delayMillis = 2_000;

        long start = System.nanoTime();
        LookupResult result = lookup(Duration.ofMillis(300)).check("11222333000181");
        long tookMillis = (System.nanoTime() - start) / 1_000_000;

        assertEquals(LookupResult.UNKNOWN, result);
        assertTrue(tookMillis < 1_500, "esperou " + tookMillis + " ms");
    }

    @Test
    void aSourceThatIsOffIsUnknown() {
        HttpStatusLookup lookup = lookup(Duration.ofMillis(500));
        server.stop(0);

        assertEquals(LookupResult.UNKNOWN, lookup.check("11222333000181"));
    }

    @Test
    void valuesThatCouldChangeTheAddressAreNeverSent() {
        HttpStatusLookup lookup = lookup(Duration.ofSeconds(3));
        List<String> attempts = new ArrayList<>(List.of("../x", "1/2", "a b", "1?x=2", "1#x", "", "a".repeat(21), "%2e%2e", "é"));
        attempts.add(null);

        for (String attempt : attempts) {
            assertEquals(LookupResult.UNKNOWN, lookup.check(attempt), String.valueOf(attempt));
        }
        assertTrue(paths.isEmpty(), "nenhum pedido saiu: " + paths);
    }

    @Test
    void theRequestIdentifiesTheApplication() {
        lookup(Duration.ofSeconds(3)).check("7732-18-5");

        assertEquals(1, userAgents.size());
        assertTrue(userAgents.get(0).startsWith("QuimiaPainel/"), userAgents.get(0));
    }
}
