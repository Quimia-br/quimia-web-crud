package com.quimia.quimiawebcrud.service.dashboard;

import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * Os textos que o dashboard monta no servidor (rótulos dos cartões, títulos dos gráficos, frases das atividades), no
 * idioma do pedido. Vêm dos arquivos messages*.properties, os mesmos que a tela usa.
 */
final class DashboardTexts {

    private final MessageSource messages;
    private final Locale locale;

    DashboardTexts(MessageSource messages, Locale locale) {
        this.messages = messages;
        this.locale = locale;
    }

    Locale locale() {
        return locale;
    }

    String get(String key, Object... args) {
        return messages.getMessage(key, args, locale);
    }

    /** "1 registro" ou "3 registros" (ou "1 record" e "3 records"). */
    String records(long count) {
        return get(count == 1 ? "dash.records.one" : "dash.records.many", count);
    }
}
