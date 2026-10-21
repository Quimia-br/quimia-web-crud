package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import com.quimia.quimiawebcrud.repository.UserRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Excluir um usuário apaga o que é dele na ordem certa e deixa o registro no log, sem a senha.
 */
class UserRemovalServiceTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AdminRepository admins = mock(AdminRepository.class);
    private final AdminLogRepository logs = mock(AdminLogRepository.class);
    private final UserRemovalService service = new UserRemovalService(entityManager, users, admins, logs);

    private User user;
    private Admin admin;

    @BeforeEach
    void setUp() {
        user = User.builder().id(7).userName("Ana Paula").email("ana@x.com").status(Status.ATIVO)
                .birthDate(LocalDate.of(2000, 1, 2)).passwordHash("$argon2id$segredo").build();
        admin = Admin.builder().id(3).userName("Adm").email("adm@quimia.com").build();
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(2);
        when(admins.findByEmailIgnoreCase("adm@quimia.com")).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("adm@quimia.com", "x", "ROLE_ADMIN"));
    }

    @AfterEach
    void clearSession() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deletesWhatBelongsToTheUserInOrderAndThenTheUser() {
        service.removeWithRecords(user);

        InOrder order = inOrder(entityManager, logs, users);
        for (UserRemovalService.Step step : UserRemovalService.STEPS) {
            order.verify(entityManager).createQuery(step.query());
        }
        order.verify(logs).save(any(AdminLog.class));
        order.verify(users).delete(user);
        order.verify(users).flush();
        verify(query, org.mockito.Mockito.times(UserRemovalService.STEPS.size())).setParameter("id", 7);
    }

    @Test
    void stepsRespectTheForeignKeysOfTheDatabase() {
        java.util.List<String> labels = UserRemovalService.STEPS.stream().map(UserRemovalService.Step::label).toList();

        assertTrue(labels.indexOf("itens de histórico") < labels.indexOf("históricos"));
        assertTrue(labels.indexOf("históricos") < labels.indexOf("estantes"));
        assertTrue(labels.indexOf("produtos nas estantes") < labels.indexOf("estantes"));
        assertTrue(labels.indexOf("itens de histórico") < labels.indexOf("produtos do usuário"),
                "histórico aponta para o par usuário+produto");
    }

    @Test
    void logsTheDeletionWithTheLoggedAdminAndNoPassword() {
        service.removeWithRecords(user);

        ArgumentCaptor<AdminLog> saved = ArgumentCaptor.forClass(AdminLog.class);
        verify(logs).save(saved.capture());
        AdminLog log = saved.getValue();
        assertEquals("usuarios", log.getAffectedTable());
        assertEquals(7, log.getRecordId());
        assertEquals(AdminAction.DELETE, log.getAction());
        assertEquals(admin, log.getAdmin());
        assertTrue(log.getPreviousData().contains("Ana Paula") && log.getPreviousData().contains("ana@x.com"));
        assertTrue(log.getPreviousData().contains("2 históricos"), log.getPreviousData());
        assertFalse(log.getPreviousData().contains("argon2") || log.getPreviousData().contains("segredo"), "sem senha no log");
    }

    @Test
    void refusesToDeleteWhenNoAdminIsLoggedIn() {
        SecurityContextHolder.clearContext();

        assertThrows(IllegalStateException.class, () -> service.removeWithRecords(user));

        verifyNoInteractions(entityManager, logs);
        verify(users, never()).delete(any(User.class));
    }

    @Test
    void refusesToDeleteWhenTheSessionAdminNoLongerExists() {
        when(admins.findByEmailIgnoreCase(eq("adm@quimia.com"))).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.removeWithRecords(user));

        verify(users, never()).delete(any(User.class));
    }
}
