package com.quimia.quimiawebcrud.service.removal;

import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.mapper.AdminLogMapper;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Quais exclusões do log ainda têm a seta de restaurar, e o que aparece na tela do texto guardado.
 */
class RestorableLogTest {

    private final AdminLogRepository repository = mock(AdminLogRepository.class);
    private final AdminLogService service = new AdminLogService(repository, new AdminLogMapper());

    private static Object[] row(int id, String table, int recordId, AdminAction action, boolean hasCopy) {
        return new Object[]{id, table, recordId, action, hasCopy};
    }

    @Test
    void onlyTheLatestActionOfARecordCanBeRestoredAndOnlyIfItIsADeletionWithACopy() {
        when(repository.findRestoreInfo()).thenReturn(List.of(
                row(1, "usuarios", 7, AdminAction.DELETE, true),     // excluído, depois restaurado, depois excluído de novo
                row(2, "usuarios", 7, AdminAction.INSERT, false),    // restauração
                row(3, "usuarios", 7, AdminAction.DELETE, true),     // a última: pode restaurar
                row(4, "produtos", 9, AdminAction.DELETE, true),     // excluído e ainda não restaurado
                row(5, "descartes_fds", 2, AdminAction.DELETE, true),
                row(6, "descartes_fds", 2, AdminAction.INSERT, false), // já restaurado
                row(7, "admins", 4, AdminAction.DELETE, false),      // exclusão antiga, sem cópia
                row(8, "estantes", 1, AdminAction.UPDATE, false)));

        assertEquals(Set.of(3, 4), service.restorableIds());
    }

    @Test
    void sameIdInDifferentTablesAreDifferentRecords() {
        when(repository.findRestoreInfo()).thenReturn(List.of(
                row(1, "produtos", 5, AdminAction.DELETE, true),
                row(2, "usuarios", 5, AdminAction.DELETE, true),
                row(3, "usuarios", 5, AdminAction.INSERT, false)));

        assertEquals(Set.of(1), service.restorableIds());
    }

    @Test
    void theScreenShowsOnlyTheSummaryNeverTheCopy() {
        String stored = RemovalLogFormat.compose("Nome: Ana\nE-mail: ana@x.com", "{\"senha_hash\":\"$argon2id$segredo\"}");
        AdminLog entry = AdminLog.builder().id(1).affectedTable("usuarios").recordId(7)
                .action(AdminAction.DELETE).previousData(stored).build();

        AdminLogResponseDTO dto = new AdminLogMapper().toResponse(entry);

        assertEquals("Nome: Ana\nE-mail: ana@x.com", dto.getPreviousData());
        assertFalse(dto.getPreviousData().contains("argon2"));
    }

    @Test
    void aRestoreLineIsStoredAsInsertButRecognisedAsARestore() {
        String note = RemovalLogFormat.restoreNote(11, "2 em historicos");
        AdminLog restore = AdminLog.builder().id(2).affectedTable("usuarios").recordId(7)
                .action(AdminAction.INSERT).previousData(note).build();
        AdminLog creation = AdminLog.builder().id(3).affectedTable("usuarios").recordId(8)
                .action(AdminAction.INSERT).previousData("Criado pelo painel").build();
        AdminLog deletion = AdminLog.builder().id(4).affectedTable("usuarios").recordId(7)
                .action(AdminAction.DELETE).previousData(note).build();

        assertTrue(new AdminLogMapper().toResponse(restore).isRestore());
        assertFalse(new AdminLogMapper().toResponse(creation).isRestore());
        assertFalse(new AdminLogMapper().toResponse(deletion).isRestore(), "só INSERT com o texto próprio conta");
    }

    @Test
    void formatHelpersHandleTextWithoutACopy() {
        assertEquals("texto antigo", RemovalLogFormat.summaryOf("texto antigo"));
        assertFalse(RemovalLogFormat.hasPayload("texto antigo"));
        assertFalse(RemovalLogFormat.hasPayload(null));
        assertTrue(RemovalLogFormat.payloadOf(RemovalLogFormat.compose("resumo", "{}")).orElseThrow().equals("{}"));
    }
}
