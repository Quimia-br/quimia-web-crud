package com.quimia.quimiawebcrud.service;

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
import com.quimia.quimiawebcrud.repository.AdminRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Excluir um registro apaga o que depende dele na ordem certa e deixa o registro no log, sem a senha.
 */
class RecordRemovalServiceTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final AdminRepository admins = mock(AdminRepository.class);
    private final AdminLogRepository logs = mock(AdminLogRepository.class);
    private final RecordRemovalService service = new RecordRemovalService(entityManager, admins, logs);

    private Admin author;

    @BeforeEach
    void setUp() {
        author = Admin.builder().id(3).userName("Adm").email("adm@quimia.com").status(Status.ATIVO).build();
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(2);
        when(admins.findByEmailIgnoreCase("adm@quimia.com")).thenReturn(Optional.of(author));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("adm@quimia.com", "x", "ROLE_ADMIN"));
    }

    @AfterEach
    void clearSession() {
        SecurityContextHolder.clearContext();
    }

    private AdminLog savedLog() {
        ArgumentCaptor<AdminLog> saved = ArgumentCaptor.forClass(AdminLog.class);
        verify(logs).save(saved.capture());
        return saved.getValue();
    }

    private void assertStepsRanInOrder(List<RecordRemovalService.Step> steps) {
        InOrder order = inOrder(entityManager);
        for (RecordRemovalService.Step step : steps) {
            order.verify(entityManager).createQuery(step.query());
        }
        verify(entityManager, times(steps.size())).createQuery(anyString());
    }

    @Test
    void userIsDeletedAfterItsRecordsAndLoggedWithoutThePassword() {
        User user = User.builder().id(7).userName("Ana Paula").email("ana@x.com").status(Status.ATIVO)
                .birthDate(LocalDate.of(2000, 1, 2)).passwordHash("$argon2id$segredo").build();

        service.removeUser(user);

        assertStepsRanInOrder(RecordRemovalService.USER_STEPS);
        InOrder order = inOrder(entityManager, logs);
        order.verify(logs).save(any(AdminLog.class));
        order.verify(entityManager).remove(user);
        order.verify(entityManager).flush();
        AdminLog log = savedLog();
        assertEquals("usuarios", log.getAffectedTable());
        assertEquals(7, log.getRecordId());
        assertEquals(AdminAction.DELETE, log.getAction());
        assertEquals(author, log.getAdmin());
        assertTrue(log.getPreviousData().contains("Ana Paula") && log.getPreviousData().contains("ana@x.com"));
        assertTrue(log.getPreviousData().contains("2 históricos"), log.getPreviousData());
        assertFalse(log.getPreviousData().contains("argon2") || log.getPreviousData().contains("segredo"), "sem senha no log");
    }

    @Test
    void userStepsRespectTheForeignKeysOfTheDatabase() {
        List<String> labels = RecordRemovalService.USER_STEPS.stream().map(RecordRemovalService.Step::label).toList();

        assertTrue(labels.indexOf("itens de histórico") < labels.indexOf("históricos"));
        assertTrue(labels.indexOf("históricos") < labels.indexOf("estantes"));
        assertTrue(labels.indexOf("produtos nas estantes") < labels.indexOf("estantes"));
        assertTrue(labels.indexOf("itens de histórico") < labels.indexOf("produtos do usuário"),
                "histórico aponta para o par usuário+produto");
    }

    @Test
    void companyIsDeletedAfterItsProductsAndEverythingLinkedToThem() {
        Company company = Company.builder().id(5).name("Limpa Bem").cnpj("12345678000190").email("c@x.com")
                .status(Status.ATIVO).passwordHash("$argon2id$segredo").build();

        service.removeCompany(company);

        assertStepsRanInOrder(RecordRemovalService.COMPANY_STEPS);
        List<String> labels = RecordRemovalService.COMPANY_STEPS.stream().map(RecordRemovalService.Step::label).toList();
        assertEquals("produtos", labels.get(labels.size() - 1), "os produtos só saem depois dos vínculos deles");
        verify(entityManager).remove(company);
        AdminLog log = savedLog();
        assertEquals("usuarios_empresas", log.getAffectedTable());
        assertTrue(log.getPreviousData().contains("Limpa Bem") && log.getPreviousData().contains("12345678000190"));
        assertTrue(log.getPreviousData().contains("2 produtos"), log.getPreviousData());
        assertFalse(log.getPreviousData().contains("segredo"));
    }

    @Test
    void productIsDeletedAfterItsDisposalShelvesUserLinksAndHistoryItems() {
        Company company = Company.builder().id(5).name("Limpa Bem").build();
        Product product = Product.builder().id(9).name("Cloro").casNumber("7681-52-9").company(company).build();

        service.removeProduct(product);

        assertStepsRanInOrder(RecordRemovalService.PRODUCT_STEPS);
        List<String> labels = RecordRemovalService.PRODUCT_STEPS.stream().map(RecordRemovalService.Step::label).toList();
        assertTrue(labels.indexOf("itens de histórico") < labels.indexOf("produtos dos usuários"));
        verify(entityManager).remove(product);
        AdminLog log = savedLog();
        assertEquals("produtos", log.getAffectedTable());
        assertTrue(log.getPreviousData().contains("Cloro") && log.getPreviousData().contains("Empresa: Limpa Bem"));
    }

    @Test
    void historyTypeIsDeletedAfterItsHistories() {
        HistoryType type = HistoryType.builder().id(4).name("Limpeza").build();

        service.removeHistoryType(type);

        assertStepsRanInOrder(RecordRemovalService.HISTORY_TYPE_STEPS);
        verify(entityManager).remove(type);
        assertEquals("tipos_historicos", savedLog().getAffectedTable());
    }

    @Test
    void disposalHasNothingDependingOnItButIsStillLogged() {
        Product product = Product.builder().id(9).name("Cloro").build();
        Disposal disposal = Disposal.builder().id(2).product(product).productDisposalMethod("Diluir").build();

        service.removeDisposal(disposal);

        verify(entityManager, never()).createQuery(anyString());
        verify(entityManager).remove(disposal);
        AdminLog log = savedLog();
        assertEquals("descartes_fds", log.getAffectedTable());
        assertTrue(log.getPreviousData().contains("Produto: Cloro") && log.getPreviousData().contains("Diluir"));
        assertTrue(log.getPreviousData().contains("Apagados junto: nada"));
    }

    @Test
    void adminIsDeletedWithItsLogRowsAndLoggedByAnotherAdmin() {
        Admin other = Admin.builder().id(8).userName("Beto").email("beto@quimia.com").status(Status.ATIVO)
                .passwordHash("$argon2id$segredo").build();

        service.removeAdmin(other);

        assertStepsRanInOrder(RecordRemovalService.ADMIN_STEPS);
        verify(entityManager).remove(other);
        AdminLog log = savedLog();
        assertEquals("admins", log.getAffectedTable());
        assertEquals(author, log.getAdmin(), "quem apagou, e não quem foi apagado");
        assertTrue(log.getPreviousData().contains("Beto") && log.getPreviousData().contains("2 registros do log"));
        assertFalse(log.getPreviousData().contains("segredo"));
    }

    @Test
    void anAdminCannotDeleteTheirOwnAccount() {
        assertThrows(InvalidDataException.class, () -> service.removeAdmin(author));

        verifyNoInteractions(entityManager, logs);
    }

    @Test
    void refusesToDeleteWhenNoAdminIsLoggedIn() {
        SecurityContextHolder.clearContext();

        assertThrows(IllegalStateException.class, () -> service.removeUser(User.builder().id(7).build()));

        verifyNoInteractions(entityManager, logs);
    }

    @Test
    void refusesToDeleteWhenTheSessionAdminNoLongerExists() {
        when(admins.findByEmailIgnoreCase("adm@quimia.com")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.removeProduct(Product.builder().id(9).build()));

        verify(entityManager, never()).remove(any());
    }
}
