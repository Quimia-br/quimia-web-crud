package com.quimia.quimiawebcrud.controller;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Regras dos filtros das listas. Um filtro em branco (ou ausente) não elimina nenhum registro.
 */
final class Filters {

    private Filters() {
    }

    /**
     * Verdadeiro se o texto buscado aparece em algum dos campos, sem diferenciar maiúsculas nem acentos.
     * Números com pontuação (CNPJ, CEP) também são comparados só pelos dígitos.
     */
    static boolean matchesText(String query, String... fields) {
        if (isBlank(query)) {
            return true;
        }
        String wanted = normalize(query);
        String wantedDigits = digitsOnly(query);
        for (String field : fields) {
            if (field == null) {
                continue;
            }
            if (normalize(field).contains(wanted)) {
                return true;
            }
            if (!wantedDigits.isEmpty() && digitsOnly(field).contains(wantedDigits)) {
                return true;
            }
        }
        return false;
    }

    static boolean sameEnum(Enum<?> value, String wanted) {
        return isBlank(wanted) || (value != null && value.name().equals(wanted));
    }

    static boolean sameId(Integer id, String wanted) {
        return isBlank(wanted) || (id != null && id.toString().equals(wanted.trim()));
    }

    static boolean sameText(String value, String wanted) {
        return isBlank(wanted) || (value != null && value.equalsIgnoreCase(wanted.trim()));
    }

    /**
     * Verdadeiro se a data cai entre "de" e "até" (inclusive). Datas inválidas ou vazias são ignoradas.
     */
    static boolean inDateRange(LocalDateTime value, String from, String to) {
        LocalDate start = toDate(from);
        LocalDate end = toDate(to);
        if (start == null && end == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        LocalDate date = value.toLocalDate();
        return (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end));
    }

    /**
     * Monta o trecho "&campo=valor" dos filtros usados, para os links da paginação manterem a busca.
     */
    static String queryString(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!"page".equals(entry.getKey()) && !isBlank(entry.getValue())) {
                query.append('&')
                        .append(UriUtils.encodeQueryParam(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(UriUtils.encodeQueryParam(entry.getValue().trim(), StandardCharsets.UTF_8));
            }
        }
        return query.toString();
    }

    static int pageNumber(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return 1;
        }
    }

    private static LocalDate toDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private static String digitsOnly(String value) {
        return value.replaceAll("\\D", "");
    }
}
