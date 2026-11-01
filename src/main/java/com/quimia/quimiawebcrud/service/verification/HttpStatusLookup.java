package com.quimia.quimiawebcrud.service.verification;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Consulta uma API pública pelo código de resposta: 200 é "existe", 404 é "não existe" e qualquer outra coisa
 * (erro do servidor, limite de pedidos, rede fora) é "não sei". Serve à BrasilAPI (CNPJ) e ao PubChem (CAS).
 */
@Slf4j
public class HttpStatusLookup implements Lookup {

    /** Só letras, números e hífen entram na URL: o valor já foi conferido antes, e aqui nada mais passa. */
    private static final Pattern SAFE_VALUE = Pattern.compile("[0-9A-Za-z-]{1,20}");

    private final HttpClient client;
    private final String urlTemplate;
    private final Duration timeout;
    private final String source;

    /**
     * @param urlTemplate endereço com um {@code %s} no lugar do valor consultado
     * @param source      nome da fonte, só para o log
     */
    public HttpStatusLookup(HttpClient client, String urlTemplate, Duration timeout, String source) {
        this.client = client;
        this.urlTemplate = urlTemplate;
        this.timeout = timeout;
        this.source = source;
    }

    @Override
    public LookupResult check(String value) {
        if (value == null || !SAFE_VALUE.matcher(value).matches()) {
            return LookupResult.UNKNOWN;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(urlTemplate.formatted(value)))
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", "QuimiaPainel/1.0 (conferencia de cadastro)")
                .GET()
                .build();
        try {
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 200) {
                return LookupResult.FOUND;
            }
            if (status == 404) {
                return LookupResult.NOT_FOUND;
            }
            log.warn("{} respondeu {} ao conferir um cadastro; o cadastro segue sem a conferência", source, status);
            return LookupResult.UNKNOWN;
        } catch (IOException e) {
            log.warn("{} não respondeu ({}); o cadastro segue sem a conferência", source, e.toString());
            return LookupResult.UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LookupResult.UNKNOWN;
        }
    }
}
