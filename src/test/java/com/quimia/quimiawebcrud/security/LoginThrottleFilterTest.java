package com.quimia.quimiawebcrud.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O filtro do login: bloqueia o IP que erra demais, recusa credenciais vazias ou enormes, limita os logins em
 * paralelo e só mexe no POST /login.
 */
class LoginThrottleFilterTest {

    /** Relógio que anda só quando o teste manda. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-01T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginThrottleFilter filter = new LoginThrottleFilter(clock);
    private final AtomicInteger reachedTheApp = new AtomicInteger();

    /** O que o Spring Security faz com a senha errada: manda de volta para /login?error. */
    private final FilterChain wrongPassword = (req, res) -> {
        reachedTheApp.incrementAndGet();
        ((MockHttpServletResponse) res).sendRedirect("/login?error");
    };

    private final FilterChain rightPassword = (req, res) -> {
        reachedTheApp.incrementAndGet();
        ((MockHttpServletResponse) res).sendRedirect("/");
    };

    private MockHttpServletRequest loginFrom(String ip, String email, String password) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.addHeader("X-Client-IP", ip);
        request.setParameter("email", email);
        request.setParameter("password", password);
        return request;
    }

    private String send(MockHttpServletRequest request, FilterChain chain) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response.getRedirectedUrl();
    }

    private void failTimes(String ip, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            assertEquals("/login?error", send(loginFrom(ip, "a" + i + "@x.com", "errada"), wrongPassword));
        }
    }

    @Test
    void anIpThatFailsTooOftenIsBlockedBeforeTheAppChecksThePassword() throws Exception {
        failTimes("1.1.1.1", LoginThrottleFilter.MAX_FAILURES_PER_IP);
        int before = reachedTheApp.get();

        assertEquals("/login?blocked", send(loginFrom("1.1.1.1", "b@x.com", "errada"), wrongPassword));
        assertEquals(before, reachedTheApp.get(), "o login bloqueado não chega a conferir a senha");
    }

    @Test
    void changingTheEmailDoesNotAvoidTheIpBlock() throws Exception {
        failTimes("1.1.1.1", LoginThrottleFilter.MAX_FAILURES_PER_IP);

        assertEquals("/login?blocked", send(loginFrom("1.1.1.1", "outro-email@x.com", "qualquer"), rightPassword));
    }

    @Test
    void anotherIpIsNotAffected() throws Exception {
        failTimes("1.1.1.1", LoginThrottleFilter.MAX_FAILURES_PER_IP);

        assertEquals("/", send(loginFrom("2.2.2.2", "a@x.com", "certa"), rightPassword));
    }

    @Test
    void aSuccessfulLoginResetsTheCount() throws Exception {
        failTimes("1.1.1.1", LoginThrottleFilter.MAX_FAILURES_PER_IP - 1);
        assertEquals("/", send(loginFrom("1.1.1.1", "a@x.com", "certa"), rightPassword));

        failTimes("1.1.1.1", LoginThrottleFilter.MAX_FAILURES_PER_IP - 1);
        assertEquals("/", send(loginFrom("1.1.1.1", "a@x.com", "certa"), rightPassword));
    }

    @Test
    void theBlockEndsAfterTheBlockTime() throws Exception {
        failTimes("1.1.1.1", LoginThrottleFilter.MAX_FAILURES_PER_IP);
        clock.advance(Duration.ofMinutes(LoginThrottleFilter.BLOCK_MINUTES).plusSeconds(1));

        assertEquals("/", send(loginFrom("1.1.1.1", "a@x.com", "certa"), rightPassword));
    }

    @Test
    void refusedCredentialsDoNotCountAsFailuresOfTheIp() throws Exception {
        for (int i = 0; i < LoginThrottleFilter.MAX_FAILURES_PER_IP + 3; i++) {
            send(loginFrom("1.1.1.1", "", "senha"), rightPassword);
        }

        assertEquals("/", send(loginFrom("1.1.1.1", "a@x.com", "certa"), rightPassword));
    }

    @Test
    void emptyOrHugeCredentialsAreRefusedWithoutReachingTheApp() throws Exception {
        assertEquals("/login?error", send(loginFrom("1.1.1.1", "", "senha"), rightPassword));
        assertEquals("/login?error", send(loginFrom("1.1.1.1", "a@x.com", ""), rightPassword));
        assertEquals("/login?error", send(loginFrom("1.1.1.1", "a".repeat(LoginThrottleFilter.MAX_EMAIL_LENGTH + 1), "senha"), rightPassword));
        assertEquals("/login?error", send(loginFrom("1.1.1.1", "a@x.com", "s".repeat(LoginThrottleFilter.MAX_PASSWORD_LENGTH + 1)), rightPassword));
        assertEquals(0, reachedTheApp.get());
    }

    @Test
    void aMissingParameterIsRefusedToo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");

        assertEquals("/login?error", send(request, rightPassword));
        assertEquals(0, reachedTheApp.get());
    }

    @Test
    void onlyThePostToLoginIsFiltered() throws Exception {
        MockHttpServletRequest page = new MockHttpServletRequest("GET", "/login");
        MockHttpServletRequest save = new MockHttpServletRequest("POST", "/admins/save");
        MockHttpServletRequest logout = new MockHttpServletRequest("POST", "/logout");

        send(page, rightPassword);
        send(save, rightPassword);
        send(logout, rightPassword);

        assertEquals(3, reachedTheApp.get(), "sem parâmetros, essas rotas passam direto");
    }

    @Test
    void theContextPathIsRespected() throws Exception {
        MockHttpServletRequest request = loginFrom("1.1.1.1", "", "");
        request.setContextPath("/admin");
        request.setRequestURI("/admin/login");

        assertEquals("/admin/login?error", send(request, rightPassword));
    }

    @Test
    void withoutTheProxyHeaderTheConnectionAddressIsUsed() throws Exception {
        for (int i = 0; i < LoginThrottleFilter.MAX_FAILURES_PER_IP; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
            request.setRemoteAddr("9.9.9.9");
            request.setParameter("email", "a@x.com");
            request.setParameter("password", "errada");
            send(request, wrongPassword);
        }
        MockHttpServletRequest again = new MockHttpServletRequest("POST", "/login");
        again.setRemoteAddr("9.9.9.9");
        again.setParameter("email", "a@x.com");
        again.setParameter("password", "errada");

        assertEquals("/login?blocked", send(again, wrongPassword));
    }

    @Test
    void aGiantIpHeaderDoesNotBreakTheFilter() throws Exception {
        String giant = "7".repeat(5_000);

        assertEquals("/login?error", send(loginFrom(giant, "a@x.com", "errada"), wrongPassword));
    }

    @Test
    void moreLoginsThanTheLimitAtTheSameTimeAreRefusedOnTheSpot() throws Exception {
        CountDownLatch insideTheApp = new CountDownLatch(LoginThrottleFilter.MAX_PARALLEL_LOGINS);
        CountDownLatch release = new CountDownLatch(1);
        FilterChain slow = (req, res) -> {
            insideTheApp.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ((MockHttpServletResponse) res).sendRedirect("/");
        };

        ExecutorService pool = Executors.newFixedThreadPool(LoginThrottleFilter.MAX_PARALLEL_LOGINS);
        try {
            for (int i = 0; i < LoginThrottleFilter.MAX_PARALLEL_LOGINS; i++) {
                int n = i;
                pool.submit(() -> send(loginFrom("3.3.3." + n, "a@x.com", "certa"), slow));
            }
            assertTrue(insideTheApp.await(10, TimeUnit.SECONDS), "os logins em andamento chegaram ao app");

            assertEquals("/login?blocked", send(loginFrom("4.4.4.4", "a@x.com", "certa"), rightPassword));
        } finally {
            release.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals("/", send(loginFrom("4.4.4.4", "a@x.com", "certa"), rightPassword), "libera quando os outros terminam");
    }
}
