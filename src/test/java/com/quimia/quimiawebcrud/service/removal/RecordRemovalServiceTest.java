package com.quimia.quimiawebcrud.service.removal;

import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.model.Disposal;
import com.quimia.quimiawebcrud.model.HistoryType;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import com.quimia.quimiawebcrud.service.CurrentAdmin;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Excluir um registro guarda uma cópia das linhas, apaga o que depende dele na ordem certa e deixa tudo no log,
 * sem mostrar a senha na parte legível.
 */
class RecordRemovalServiceTest {

    private static final String COPY_MARK = "jsonb_agg";

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final CurrentAdmin currentAdmin = mock(CurrentAdmin.class);
    private final AdminLogRepository logs = mock(AdminLogRepository.class);
    private final RecordRemovalService service = new RecordRemovalService(entityManager, currentAdmin, logs);

    private final List<String> sql = new ArrayList<>();
    private Admin author;

    @BeforeEach
    void setUp() {
        author = Admin.builder().id(3).userName("Adm").email("adm@quimia.com").status(Status.ATIVO).build();
        when(currentAdmin.require()).thenReturn(author);
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            sql.add(invocation.getArgument(0));
            return query;
        });
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn("[{\"id_usuario\":7,\"senha_hash\":\"$argon2id$segredo\"}]");
        when(query.executeUpdate()).thenReturn(2);
    }

    private AdminLog savedLog() {
        ArgumentCaptor<AdminLog> saved = ArgumentCaptor.forClass(AdminLog.class);
        verify(logs).save(saved.capture());
        return saved.getValue();
    }

    private List<String> copies() {
        return sql.stream().filter(s -> s.contains(COPY_MARK)).toList();
    }

    private List<String> deletes() {
        return sql.stream().filter(s -> s.startsWith("delete from ")).toList();
    }

    /** A cópia é tirada na ordem do plano e as exclusões acontecem na ordem inversa. */
    private void assertPlanWasFollowed(List<RecordRemovalService.Part> parts) {
        assertEquals(parts.size(), copies().size());
        assertEquals(parts.size(), deletes().size());
        for (int i = 0; i < parts.size(); i++) {
            assertTrue(copies().get(i).contains("from " + parts.get(i).table() + " t where"), "cópia " + i);
            assertTrue(deletes().get(parts.size() - 1 - i).startsWith("delete from " + parts.get(i).table() + " where"),
                    "exclusão " + i);
        }
        assertTrue(sql.indexOf(copies().get(copies().size() - 1)) < sql.indexOf(deletes().get(0)),
                "a cópia vem antes de qualquer exclusão");
    }

    @Test
    void userIsCopiedThenDeletedAfterItsRecordsAndLoggedWithTheCopy() {
        User user = User.builder().id(7).userName("Ana Paula").email("ana@x.com").status(Status.ATIVO)
                .birthDate(LocalDate.of(2000, 1, 2)).passwordHash("$argon2id$segredo").build();

        service.removeUser(user);

        assertPlanWasFollowed(RecordRemovalService.USER_PARTS);
        verify(entityManager).detach(user);
        AdminLog log = savedLog();
        assertEquals("usuarios", log.getAffectedTable());
        assertEquals(7, log.getRecordId());
        assertEquals(AdminAction.DELETE, log.getAction());
        assertEquals(author, log.getAdmin());

        String summary = RemovalLogFormat.summaryOf(log.getPreviousData());
        assertTrue(summary.contains("Ana Paula") && summary.contains("ana@x.com"));
        assertTrue(summary.contains("2 históricos"), summary);
        assertFalse(summary.contains("argon2") || summary.contains("segredo"), "sem senha no que aparece na tela");

        JsonNode copy = RecordRemovalService.JSON.readTree(RemovalLogFormat.payloadOf(log.getPreviousData()).orElseThrow());
        assertEquals("usuarios", copy.get("table").asString());
        assertEquals(7, copy.get("id").asInt());
        assertEquals(RecordRemovalService.USER_PARTS.size(), copy.get("parts").size());
        assertEquals("usuarios", copy.get("parts").get(0).get("table").asString());
        assertTrue(copy.toString().contains("senha_hash"), "o hash vai na cópia para a senha voltar igual");
    }

    @Test
    void thePlansListTheRecordFirstSoItCanBeReinsertedBeforeWhoDependsOnIt() {
        List<String> users = RecordRemovalService.USER_PARTS.stream().map(RecordRemovalService.Part::table).toList();

        assertEquals("usuarios", users.get(0));
        assertTrue(users.indexOf("estantes") < users.indexOf("produtos_estantes"));
        assertTrue(users.indexOf("estantes") < users.indexOf("historicos"));
        assertTrue(users.indexOf("historicos") < users.indexOf("historicos_produtos"));
        assertTrue(users.indexOf("produtos_usuarios") < users.indexOf("historicos_produtos"),
                "o item de histórico aponta para o par usuário+produto");
        assertEquals("usuarios_empresas", RecordRemovalService.COMPANY_PARTS.get(0).table());
        assertEquals("produtos", RecordRemovalService.COMPANY_PARTS.get(1).table());
        assertEquals("produtos", RecordRemovalService.PRODUCT_PARTS.get(0).table());
        assertEquals("tipos_historicos", RecordRemovalService.HISTORY_TYPE_PARTS.get(0).table());
        assertEquals("admins", RecordRemovalService.ADMIN_PARTS.get(0).table());
    }

    @Test
    void companyIsDeletedAfterItsProductsAndEverythingLinkedToThem() {
        Company company = Company.builder().id(5).name("Limpa Bem").cnpj("12345678000190").email("c@x.com")
                .status(Status.ATIVO).passwordHash("$argon2id$segredo").build();

        service.removeCompany(company);

        assertPlanWasFollowed(RecordRemovalService.COMPANY_PARTS);
        AdminLog log = savedLog();
        assertEquals("usuarios_empresas", log.getAffectedTable());
        String summary = RemovalLogFormat.summaryOf(log.getPreviousData());
        assertTrue(summary.contains("Limpa Bem") && summary.contains("12345678000190"));
        assertTrue(summary.contains("2 produtos"), summary);
        assertFalse(summary.contains("segredo"));
    }

    @Test
    void productIsDeletedAfterItsDisposalShelvesUserLinksAndHistoryItems() {
        Company company = Company.builder().id(5).name("Limpa Bem").build();
        Product product = Product.builder().id(9).name("Cloro").casNumber("7681-52-9").company(company).build();

        service.removeProduct(product);

        assertPlanWasFollowed(RecordRemovalService.PRODUCT_PARTS);
        AdminLog log = savedLog();
        assertEquals("produtos", log.getAffectedTable());
        assertTrue(log.getPreviousData().contains("Cloro") && log.getPreviousData().contains("Empresa: Limpa Bem"));
    }

    @Test
    void historyTypeIsDeletedAfterItsHistories() {
        service.removeHistoryType(HistoryType.builder().id(4).name("Limpeza").build());

        assertPlanWasFollowed(RecordRemovalService.HISTORY_TYPE_PARTS);
        assertEquals("tipos_historicos", savedLog().getAffectedTable());
    }

    @Test
    void disposalHasNoDependentsButIsStillCopiedAndLogged() {
        Product product = Product.builder().id(9).name("Cloro").build();
        Disposal disposal = Disposal.builder().id(2).product(product).productDisposalMethod("Diluir").build();

        service.removeDisposal(disposal);

        assertPlanWasFollowed(RecordRemovalService.DISPOSAL_PARTS);
        AdminLog log = savedLog();
        assertEquals("descartes_fds", log.getAffectedTable());
        String summary = RemovalLogFormat.summaryOf(log.getPreviousData());
        assertTrue(summary.contains("Produto: Cloro") && summary.contains("Diluir"));
        assertTrue(summary.contains("Apagados junto: nada"));
        assertTrue(RemovalLogFormat.hasPayload(log.getPreviousData()));
    }

    @Test
    void adminIsDeletedWithItsLogRowsAndLoggedByAnotherAdmin() {
        Admin other = Admin.builder().id(8).userName("Beto").email("beto@quimia.com").status(Status.ATIVO)
                .passwordHash("$argon2id$segredo").build();

        service.removeAdmin(other);

        assertPlanWasFollowed(RecordRemovalService.ADMIN_PARTS);
        AdminLog log = savedLog();
        assertEquals("admins", log.getAffectedTable());
        assertEquals(author, log.getAdmin(), "quem apagou, e não quem foi apagado");
        String summary = RemovalLogFormat.summaryOf(log.getPreviousData());
        assertTrue(summary.contains("Beto") && summary.contains("2 registros do log"));
        assertFalse(summary.contains("segredo"));
    }

    @Test
    void anAdminCannotDeleteTheirOwnAccount() {
        assertThrows(InvalidDataException.class, () -> service.removeAdmin(author));

        verifyNoInteractions(entityManager, logs);
    }

    @Test
    void refusesToDeleteWhenNoAdminIsLoggedIn() {
        when(currentAdmin.require()).thenThrow(new IllegalStateException("sem admin"));

        assertThrows(IllegalStateException.class, () -> service.removeUser(User.builder().id(7).build()));

        verifyNoInteractions(entityManager, logs);
    }

    @Test
    void nothingIsLoggedWhenADeletionFails() {
        when(query.executeUpdate()).thenThrow(new IllegalStateException("o banco recusou"));

        assertThrows(IllegalStateException.class, () -> service.removeProduct(Product.builder().id(9).build()));

        verify(logs, never()).save(any());
    }
}
