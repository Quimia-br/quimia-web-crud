package com.quimia.quimiawebcrud.service.removal;

import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.exception.ResourceNotFoundException;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import com.quimia.quimiawebcrud.service.CurrentAdmin;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Restaurar reinsere, na ordem do plano, as linhas guardadas no log e registra a restauração.
 */
class RecordRestoreServiceTest {

    private static final String COPY = "{\"version\":1,\"table\":\"usuarios\",\"id\":7,\"parts\":["
            + "{\"table\":\"usuarios\",\"rows\":[{\"id_usuario\":7,\"nome\":\"Ana\",\"senha_hash\":\"$argon2id$x\","
            + "\"url_foto\":\"http://app.test/uploads/photos/abc.png\"}]},"
            + "{\"table\":\"localizacoes\",\"rows\":[]},"
            + "{\"table\":\"historicos\",\"rows\":[{\"id_historico\":1},{\"id_historico\":2}]}]}";

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final CurrentAdmin currentAdmin = mock(CurrentAdmin.class);
    private final AdminLogRepository logs = mock(AdminLogRepository.class);
    private final AdminLogService adminLogService = mock(AdminLogService.class);
    private final RecordRestoreService service = new RecordRestoreService(entityManager, currentAdmin, logs, adminLogService);

    private final List<String> sql = new ArrayList<>();
    private final List<String> rowsSent = new ArrayList<>();
    private Admin author;

    @BeforeEach
    void setUp() {
        author = Admin.builder().id(3).userName("Adm").email("adm@quimia.com").build();
        when(currentAdmin.require()).thenReturn(author);
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            sql.add(invocation.getArgument(0));
            return query;
        });
        when(query.setParameter(eq("rows"), any())).thenAnswer(invocation -> {
            rowsSent.add(invocation.getArgument(1));
            return query;
        });
        when(query.executeUpdate()).thenReturn(1);
        when(adminLogService.restorableIds()).thenReturn(Set.of(11));
        when(logs.findById(11)).thenReturn(Optional.of(deletion(11, AdminAction.DELETE, COPY)));
    }

    private static AdminLog deletion(int id, AdminAction action, String copy) {
        String stored = copy == null ? "só um resumo" : RemovalLogFormat.compose("Nome: Ana", copy);
        return AdminLog.builder().id(id).affectedTable("usuarios").recordId(7).action(action).previousData(stored).build();
    }

    @Test
    void reinsertsTheRowsInPlanOrderKeepingTheIdsAndLogsTheRestore() {
        service.restore(11);

        assertEquals(2, sql.size(), "a tabela sem linhas é pulada");
        assertEquals("insert into usuarios overriding system value select * from jsonb_populate_recordset("
                + "cast(null as usuarios), cast(:rows as jsonb))", sql.get(0));
        assertTrue(sql.get(1).startsWith("insert into historicos "));
        assertTrue(rowsSent.get(1).contains("\"id_historico\":1") && rowsSent.get(1).contains("\"id_historico\":2"));

        ArgumentCaptor<AdminLog> saved = ArgumentCaptor.forClass(AdminLog.class);
        verify(logs).save(saved.capture());
        AdminLog log = saved.getValue();
        assertEquals(AdminAction.INSERT, log.getAction());
        assertTrue(RemovalLogFormat.isRestore(log.getAction(), log.getPreviousData()), "reconhecida como restauração");
        assertEquals("usuarios", log.getAffectedTable());
        assertEquals(7, log.getRecordId());
        assertEquals(author, log.getAdmin());
        assertTrue(log.getPreviousData().contains("#11") && log.getPreviousData().contains("2 em historicos"),
                log.getPreviousData());
    }

    @Test
    void thePasswordHashComesBackButTheDeletedPhotoFileDoesNot() {
        service.restore(11);

        JsonNode user = RecordRemovalService.JSON.readTree(rowsSent.get(0)).get(0);
        assertEquals("$argon2id$x", user.get("senha_hash").asString(), "a senha volta igual");
        assertTrue(user.get("url_foto").isNull(), "o arquivo da foto foi apagado na exclusão");
    }

    @Test
    void refusesWhenTheRecordWasAlreadyRestoredOrIsNotTheLatestAction() {
        when(adminLogService.restorableIds()).thenReturn(Set.of());

        assertThrows(InvalidDataException.class, () -> service.restore(11));

        verifyNoInteractions(entityManager);
        verify(logs, never()).save(any());
    }

    @Test
    void refusesEntriesThatAreNotDeletionsOrHaveNoCopy() {
        when(logs.findById(12)).thenReturn(Optional.of(deletion(12, AdminAction.INSERT, COPY)));
        when(logs.findById(13)).thenReturn(Optional.of(deletion(13, AdminAction.DELETE, null)));
        when(adminLogService.restorableIds()).thenReturn(Set.of(12, 13));

        assertThrows(InvalidDataException.class, () -> service.restore(12));
        assertThrows(InvalidDataException.class, () -> service.restore(13));

        verifyNoInteractions(entityManager);
    }

    @Test
    void refusesACopyWithATableOutsideTheKnownPlansBeforeInsertingAnything() {
        String tampered = "{\"version\":1,\"table\":\"usuarios\",\"id\":7,\"parts\":["
                + "{\"table\":\"usuarios\",\"rows\":[{\"id_usuario\":7}]},"
                + "{\"table\":\"pg_authid\",\"rows\":[{\"x\":1}]}]}";
        when(logs.findById(14)).thenReturn(Optional.of(deletion(14, AdminAction.DELETE, tampered)));
        when(adminLogService.restorableIds()).thenReturn(Set.of(14));

        assertThrows(InvalidDataException.class, () -> service.restore(14));

        assertTrue(sql.isEmpty(), "nada é inserido");
    }

    @Test
    void refusesACorruptedCopyBeforeInsertingAnything() {
        String[] corrupted = {"não é json", "{}", "{\"parts\":{}}", "{\"parts\":[{\"rows\":[]}]}",
                "{\"parts\":[{\"table\":\"usuarios\"}]}"};
        when(adminLogService.restorableIds()).thenReturn(Set.of(15));
        for (String copy : corrupted) {
            when(logs.findById(15)).thenReturn(Optional.of(deletion(15, AdminAction.DELETE, copy)));

            InvalidDataException error = assertThrows(InvalidDataException.class, () -> service.restore(15), copy);
            assertTrue(error.getMessage().contains("corrompida"), copy);
        }

        assertTrue(sql.isEmpty(), "nada é inserido");
    }

    @Test
    void unknownLogEntryIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.restore(99));
    }

    @Test
    void refusesWithoutALoggedInAdmin() {
        when(currentAdmin.require()).thenThrow(new IllegalStateException("sem admin"));

        assertThrows(IllegalStateException.class, () -> service.restore(11));

        assertFalse(sql.iterator().hasNext());
    }
}
