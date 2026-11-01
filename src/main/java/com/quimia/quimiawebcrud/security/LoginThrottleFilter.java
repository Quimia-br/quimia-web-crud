package com.quimia.quimiawebcrud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Protege o envio do formulário de login (POST /login) de três jeitos, todos antes do cálculo do hash Argon2,
 * que é o passo caro em memória e em CPU:
 * <ul>
 *   <li>recusa e-mail ou senha vazios ou grandes demais (volta para /login?error, como qualquer login errado);</li>
 *   <li>bloqueia por {@value #BLOCK_MINUTES} minutos o IP que errou {@value #MAX_FAILURES_PER_IP} logins seguidos
 *       (o bloqueio por e-mail, em {@link LoginAttemptService}, não pega quem troca de e-mail a cada tentativa);</li>
 *   <li>deixa só {@value #MAX_PARALLEL_LOGINS} logins sendo conferidos ao mesmo tempo: o excedente é recusado na hora,
 *       em vez de acumular hashes na memória do servidor.</li>
 * </ul>
 * Quem é bloqueado por IP ou por excesso de logins em paralelo volta para /login?blocked. Os contadores ficam só na memória (zeram ao reiniciar). Não é um
 * {@code @Component}: o {@link SecurityConfig} o cria e o põe na cadeia, para não ser registrado duas vezes.
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

    static final int MAX_FAILURES_PER_IP = 10;
    static final int BLOCK_MINUTES = 10;
    static final int MAX_PARALLEL_LOGINS = 2;
    static final int MAX_EMAIL_LENGTH = 150;
    static final int MAX_PASSWORD_LENGTH = 200;

    private static final Duration BLOCK_TIME = Duration.ofMinutes(BLOCK_MINUTES);
    private static final int MAX_TRACKED_IPS = 10_000;
    private static final int MAX_IP_LENGTH = 45;
    private static final String LOGIN_PATH = "/login";

    /** Erros seguidos de um IP e quando foi o último. */
    private record Failures(int count, Instant last) {
    }

    private final Map<String, Failures> failuresByIp = new ConcurrentHashMap<>();
    private final Semaphore parallelLogins = new Semaphore(MAX_PARALLEL_LOGINS);
    private final Clock clock;

    public LoginThrottleFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !request.getRequestURI().equals(request.getContextPath() + LOGIN_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = clientIp(request);
        if (!hasUsableCredentials(request)) {
            // Mesma mensagem de qualquer login errado: quem manda campo vazio ou gigante não ganha pista nenhuma
            response.sendRedirect(request.getContextPath() + LOGIN_PATH + "?error");
            return;
        }
        if (isBlocked(ip) || !parallelLogins.tryAcquire()) {
            response.sendRedirect(request.getContextPath() + LOGIN_PATH + "?blocked");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            parallelLogins.release();
        }
        registerOutcome(ip, response);
    }

    /** E-mail e senha presentes e de tamanho razoável; o que passa disso nunca é um login de verdade. */
    private boolean hasUsableCredentials(HttpServletRequest request) {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        return email != null && !email.isBlank() && email.length() <= MAX_EMAIL_LENGTH
                && password != null && !password.isEmpty() && password.length() <= MAX_PASSWORD_LENGTH;
    }

    private boolean isBlocked(String ip) {
        Failures current = failuresByIp.get(ip);
        if (current == null) {
            return false;
        }
        if (isExpired(current)) {
            failuresByIp.remove(ip);
            return false;
        }
        return current.count() >= MAX_FAILURES_PER_IP;
    }

    /** Login recusado (redirecionado para ?error) soma um erro do IP; login aceito zera. */
    private void registerOutcome(String ip, HttpServletResponse response) {
        String location = response.getHeader("Location");
        boolean failed = location != null && location.contains(LOGIN_PATH + "?error");
        if (!failed) {
            failuresByIp.remove(ip);
            return;
        }
        if (failuresByIp.size() >= MAX_TRACKED_IPS) {
            failuresByIp.values().removeIf(this::isExpired);
            if (failuresByIp.size() >= MAX_TRACKED_IPS && !failuresByIp.containsKey(ip)) {
                return;
            }
        }
        Instant now = clock.instant();
        failuresByIp.merge(ip, new Failures(1, now),
                (old, fresh) -> isExpired(old) ? fresh : new Failures(old.count() + 1, now));
    }

    private boolean isExpired(Failures value) {
        return value.last().plus(BLOCK_TIME).isBefore(clock.instant());
    }

    /**
     * IP de quem acessou. O Worker da Cloudflare manda X-Client-IP com o IP real; sem ele (acesso direto, testes)
     * vale o CF-Connecting-IP e, por último, o endereço da conexão. O valor é cortado para não encher a memória.
     */
    private String clientIp(HttpServletRequest request) {
        String ip = firstNonBlank(request.getHeader("X-Client-IP"), request.getHeader("CF-Connecting-IP"),
                request.getRemoteAddr());
        ip = ip.trim();
        return ip.length() > MAX_IP_LENGTH ? ip.substring(0, MAX_IP_LENGTH) : ip;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown";
    }
}
