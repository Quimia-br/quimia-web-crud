package com.quimia.quimiawebcrud;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

/**
 * Os mesmos arquivos de textos que o painel usa (messages*.properties), para os testes que montam o serviço à mão
 * conferirem o texto de verdade, em português ou em inglês.
 */
public final class TestMessages {

    public static final Locale PT = Locale.forLanguageTag("pt-BR");
    public static final Locale EN = Locale.ENGLISH;

    private TestMessages() {
    }

    public static MessageSource source() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
