package com.quimia.quimiawebcrud.service;

import java.util.Optional;

/**
 * Formato do campo "dado anterior" do log de exclusões: um resumo legível e, depois do marcador, a cópia
 * completa (JSON) usada para restaurar o registro. A tela só mostra o resumo.
 */
public final class RemovalLogFormat {

    static final String MARKER = "\n---dados-para-restaurar---\n";

    private RemovalLogFormat() {
    }

    public static String compose(String summary, String payloadJson) {
        return summary + MARKER + payloadJson;
    }

    /** O que aparece na tela: tudo antes do marcador (ou o texto inteiro, se não houver cópia). */
    public static String summaryOf(String stored) {
        if (stored == null) {
            return null;
        }
        int at = stored.indexOf(MARKER);
        return at < 0 ? stored : stored.substring(0, at);
    }

    public static Optional<String> payloadOf(String stored) {
        if (stored == null) {
            return Optional.empty();
        }
        int at = stored.indexOf(MARKER);
        return at < 0 ? Optional.empty() : Optional.of(stored.substring(at + MARKER.length()));
    }

    public static boolean hasPayload(String stored) {
        return payloadOf(stored).isPresent();
    }
}
