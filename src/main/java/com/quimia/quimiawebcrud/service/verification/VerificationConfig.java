package com.quimia.quimiawebcrud.service.verification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/**
 * Liga as conferências externas aos endereços de verdade: o DNS para o e-mail, a BrasilAPI (dados públicos do CNPJ na
 * Receita Federal) e o PubChem (substâncias químicas, procuradas pelo número CAS). Cada conferência e o tempo máximo
 * de espera vêm das propriedades {@code app.verify.*}.
 */
@Configuration
public class VerificationConfig {

    @Bean
    ExternalChecks externalChecks(@Value("${app.verify.email:true}") boolean checkEmail,
                                  @Value("${app.verify.cnpj:true}") boolean checkCnpj,
                                  @Value("${app.verify.cas:true}") boolean checkCas,
                                  @Value("${app.verify.timeout-ms:4000}") int timeoutMillis) {
        Duration timeout = Duration.ofMillis(timeoutMillis);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return new ExternalChecks(
                new EmailDomainLookup(timeoutMillis),
                new HttpStatusLookup(client, "https://brasilapi.com.br/api/cnpj/v1/%s", timeout, "BrasilAPI"),
                new HttpStatusLookup(client, "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/%s/cids/JSON", timeout, "PubChem"),
                checkEmail, checkCnpj, checkCas, Clock.systemUTC());
    }
}
