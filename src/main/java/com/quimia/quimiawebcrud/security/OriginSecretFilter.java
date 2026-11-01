package com.quimia.quimiawebcrud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Só deixa passar pedidos que vieram pelo proxy (Cloudflare). O proxy acrescenta o cabeçalho {@code X-Origin-Secret}
 * com um segredo combinado; quem acessar o app direto, sem passar por ele, recebe 403. Assim as proteções que ficam
 * no proxy (limite de tentativas de login, Cloudflare Access) não podem ser contornadas.
 * <p>
 * Sem segredo configurado o filtro fica desligado (computador de quem desenvolve). O caminho {@value #HEALTH_PATH}
 * fica de fora, para o health check do Render, que não passa pelo proxy.
 */
@Slf4j
public class OriginSecretFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Origin-Secret";
    public static final String HEALTH_PATH = "/health";

    private final byte[] secret;
    private final AtomicBoolean warned = new AtomicBoolean();

    public OriginSecretFilter(String secret) {
        this.secret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return secret.length == 0 || HEALTH_PATH.equals(pathInsideTheApp(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String sent = request.getHeader(HEADER);
        // Compara em tempo constante, para ninguém descobrir o segredo pelo tempo da resposta
        if (sent != null && MessageDigest.isEqual(secret, sent.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        if (warned.compareAndSet(false, true)) {
            log.warn("Pedido recusado por falta do cabeçalho {} (ex.: {} {}). Se o painel inteiro der 403, confira se o "
                    + "Worker da Cloudflare envia o mesmo segredo de ORIGIN_SECRET.", HEADER, request.getMethod(),
                    pathInsideTheApp(request));
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    /** O caminho sem o prefixo do app (ex.: /admin/health vira /health). */
    private static String pathInsideTheApp(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
