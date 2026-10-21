package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.exception.ResourceNotFoundException;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Desfaz uma exclusão: reinsere, a partir da cópia guardada no log, o registro e tudo que foi apagado junto,
 * com os mesmos ids. A restauração fica registrada no log, e a seta de restaurar some dessa linha.
 * Tudo acontece em uma transação: se algo falhar (por exemplo, o e-mail já foi usado por outro registro),
 * nada é restaurado.
 */
@Service
@RequiredArgsConstructor
public class RecordRestoreService {

    /** Insere as linhas do JSON na tabela, mantendo os ids originais. */
    private static final String INSERT = "insert into %s overriding system value "
            + "select * from jsonb_populate_recordset(cast(null as %s), cast(:rows as jsonb))";

    /** Tabelas com foto guardada pelo sistema: o arquivo foi apagado na exclusão, então a foto não volta. */
    private static final Set<String> PHOTO_TABLES = Set.of("usuarios", "usuarios_empresas");

    private static final String PHOTO_COLUMN = "url_foto";

    /** O texto do log vem do banco; só tabelas dos planos conhecidos podem ser reinseridas. */
    private static final Set<String> KNOWN_TABLES = RecordRemovalService.allParts().stream()
            .map(RecordRemovalService.Part::table).collect(Collectors.toSet());

    private final EntityManager entityManager;
    private final CurrentAdmin currentAdmin;
    private final AdminLogRepository logs;
    private final AdminLogService adminLogService;

    @Transactional
    public void restore(Integer logId) {
        Admin author = currentAdmin.require();
        AdminLog entry = logs.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro do log não encontrado com o ID: " + logId));
        String copy = RemovalLogFormat.payloadOf(entry.getPreviousData()).orElse(null);
        if (entry.getAction() != AdminAction.DELETE || copy == null
                || !adminLogService.restorableIds().contains(logId)) {
            throw new InvalidDataException("Este registro já foi restaurado ou não pode ser restaurado.");
        }

        JsonNode parts = RecordRemovalService.JSON.readTree(copy).get("parts");
        for (JsonNode part : parts) {
            String table = part.get("table").asString();
            if (!KNOWN_TABLES.contains(table)) {
                throw new InvalidDataException("A cópia guardada no log tem uma tabela desconhecida: " + table);
            }
        }

        List<String> restored = new ArrayList<>();
        boolean first = true;
        for (JsonNode part : parts) {
            String table = part.get("table").asString();
            JsonNode rows = part.get("rows");
            if (!rows.isEmpty()) {
                if (PHOTO_TABLES.contains(table)) {
                    dropStoredPhotos(rows);
                }
                entityManager.createNativeQuery(INSERT.formatted(table, table))
                        .setParameter("rows", rows.toString()).executeUpdate();
                if (!first) {
                    restored.add(rows.size() + " em " + table);
                }
            }
            first = false;
        }

        logs.save(AdminLog.builder()
                .admin(author)
                .affectedTable(entry.getAffectedTable())
                .recordId(entry.getRecordId())
                .action(AdminAction.INSERT)
                .previousData(RemovalLogFormat.restoreNote(logId, restored.isEmpty() ? "nada" : String.join(", ", restored)))
                .build());
    }

    private void dropStoredPhotos(JsonNode rows) {
        for (JsonNode row : rows) {
            JsonNode photo = row.get(PHOTO_COLUMN);
            if (photo != null && photo.isString() && photo.asString().contains(PhotoStorageService.URL_PREFIX)) {
                ((ObjectNode) row).putNull(PHOTO_COLUMN);
            }
        }
    }
}
