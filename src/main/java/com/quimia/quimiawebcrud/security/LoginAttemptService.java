package com.quimia.quimiawebcrud.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção básica contra tentativas de adivinhar senhas: depois de {@value #MAX_FAILURES} erros seguidos,
 * a conta fica bloqueada por 15 minutos. Os contadores ficam só na memória (zeram ao reiniciar o servidor).
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration BLOCK_TIME = Duration.ofMinutes(15);
    private static final int MAX_TRACKED = 10_000;

    private record Attempts(int failures, Instant lastFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    /**
     * A conta está bloqueada por erros seguidos de senha? Passado o tempo de bloqueio, o contador é zerado.
     */
    public boolean isBlocked(String email) {
        Attempts current = attempts.get(key(email));
        if (current == null) {
            return false;
        }
        if (isExpired(current)) {
            attempts.remove(key(email));
            return false;
        }
        return current.failures() >= MAX_FAILURES;
    }

    /** Soma um erro de senha para o e-mail (o Spring Security publica o evento a cada falha de login). */
    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String key = key(event.getAuthentication().getName());
        if (attempts.size() >= MAX_TRACKED) {
            attempts.values().removeIf(this::isExpired);
            if (attempts.size() >= MAX_TRACKED && !attempts.containsKey(key)) {
                return;
            }
        }
        attempts.merge(key, new Attempts(1, Instant.now()),
                (old, fresh) -> isExpired(old) ? fresh : new Attempts(old.failures() + 1, fresh.lastFailure()));
    }

    /** Login correto: zera os erros do e-mail. */
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        attempts.remove(key(event.getAuthentication().getName()));
    }

    private boolean isExpired(Attempts value) {
        return value.lastFailure().plus(BLOCK_TIME).isBefore(Instant.now());
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
