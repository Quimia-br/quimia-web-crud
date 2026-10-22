package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.model.enums.AdminAction;

import java.util.Optional;

/**
 * Formato do campo "dado anterior" do log de exclusões: um resumo legível e, depois do marcador, a cópia
 * completa (JSON) usada para restaurar o registro. A tela só mostra o resumo.
 */
public final class RemovalLogFormat {

    /** Começo do texto das linhas de restauração: o banco só aceita INSERT/UPDATE/DELETE, então é assim que se reconhece. */
    static final String RESTORE_PREFIX = "Restaurado a partir do registro #";

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

    /** Texto da linha do log que registra uma restauração (gravada como INSERT). */
    public static String restoreNote(Integer restoredLogId, String alsoRestored) {
        return RESTORE_PREFIX + restoredLogId + " do log. Voltaram junto: " + alsoRestored + ".";
    }

    /** Linha de restauração: gravada como INSERT, mas com o texto de {@link #restoreNote}. */
    public static boolean isRestore(AdminAction action, String stored) {
        return action == AdminAction.INSERT && stored != null && stored.startsWith(RESTORE_PREFIX);
    }
}
