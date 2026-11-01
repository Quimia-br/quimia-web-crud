package com.quimia.quimiawebcrud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * O idioma do painel: a escolha feita nas configurações fica num cookie (quimia.lang, por um ano) e, enquanto não
 * houver escolha, vale o português. O idioma do navegador de quem acessa não é usado de propósito, para o painel
 * abrir sempre do mesmo jeito. Implementa {@link WebMvcConfigurer} só para o teste de tela carregar esta classe também.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    public static final Locale PORTUGUESE = Locale.forLanguageTag("pt-BR");
    public static final Locale ENGLISH = Locale.ENGLISH;
    /** Os idiomas que o painel oferece, na ordem em que aparecem nas configurações. */
    public static final List<Locale> SUPPORTED = List.of(PORTUGUESE, ENGLISH);

    /**
     * O Spring procura este bean pelo nome "localeResolver". O cookie só vale para o painel (mesmo prefixo da
     * aplicação), não para o resto de quimia.tech, e só trafega por HTTPS quando o cookie de login também é seguro.
     */
    @Bean
    public LocaleResolver localeResolver(@Value("${server.servlet.context-path:}") String contextPath,
                                         @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        CookieLocaleResolver resolver = new CookieLocaleResolver("quimia.lang");
        resolver.setDefaultLocale(PORTUGUESE);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookiePath(contextPath.isBlank() ? "/" : contextPath);
        resolver.setCookieHttpOnly(true);
        resolver.setCookieSecure(secure);
        resolver.setCookieSameSite("Lax");
        return resolver;
    }

    /** Só os idiomas oferecidos valem; qualquer outro texto vira o português. */
    public static Locale supported(String tag) {
        if (tag == null) {
            return PORTUGUESE;
        }
        return SUPPORTED.stream()
                .filter(locale -> locale.toLanguageTag().equalsIgnoreCase(tag.trim()))
                .findFirst()
                .orElse(PORTUGUESE);
    }
}
