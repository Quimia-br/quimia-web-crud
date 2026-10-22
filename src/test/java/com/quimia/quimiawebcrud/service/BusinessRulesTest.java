package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.AdminRequestDTO;
import com.quimia.quimiawebcrud.dto.DisposalRequestDTO;
import com.quimia.quimiawebcrud.dto.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.UserRequestDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.AdminMapper;
import com.quimia.quimiawebcrud.mapper.DisposalMapper;
import com.quimia.quimiawebcrud.mapper.ProductMapper;
import com.quimia.quimiawebcrud.mapper.UserMapper;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import com.quimia.quimiawebcrud.repository.DisposalRepository;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import com.quimia.quimiawebcrud.repository.UserRepository;
import com.quimia.quimiawebcrud.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras do modelo aplicadas nos cadastros: dados repetidos e referências inexistentes são recusados antes de
 * chegar ao banco, e toda criação e edição fica registrada no log de edições.
 */
class BusinessRulesTest {

    private final CurrentAdmin currentAdmin = mock(CurrentAdmin.class);
    private final AdminLogRepository logs = mock(AdminLogRepository.class);
    private final EditLog editLog = new EditLog(currentAdmin, logs);
    private final Admin me = Admin.builder().id(3).userName("Eu").email("eu@quimia.com").status(Status.ATIVO).build();

    private final UserRepository users = mock(UserRepository.class);
    private final UserService userService = new UserService(users, new UserMapper(), new PasswordHasher(),
            mock(PhotoStorageService.class), mock(RecordRemovalService.class), editLog);

    private final ProductRepository products = mock(ProductRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final ProductService productService = new ProductService(products, new ProductMapper(), companies,
            mock(RecordRemovalService.class), editLog);

    private final DisposalRepository disposals = mock(DisposalRepository.class);
    private final DisposalService disposalService = new DisposalService(disposals, new DisposalMapper(), products,
            mock(RecordRemovalService.class), editLog);

    private final AdminRepository admins = mock(AdminRepository.class);
    private final AdminService adminService = new AdminService(admins, new AdminMapper(), new PasswordHasher(),
            mock(RecordRemovalService.class), editLog, currentAdmin);

    @BeforeEach
    void setUp() {
        when(currentAdmin.require()).thenReturn(me);
    }

    private static UserRequestDTO user(String email) {
        return UserRequestDTO.builder().userName("Ana").email(email).password("s3nha-forte").status(Status.ATIVO).build();
    }

    private AdminLog loggedEntry() {
        ArgumentCaptor<AdminLog> entry = ArgumentCaptor.forClass(AdminLog.class);
        verify(logs).save(entry.capture());
        return entry.getValue();
    }

    @Test
    void creatingAUserWithAnEmailInUseIsADuplicate() {
        when(users.existsByEmailIgnoreCase("ana@x.com")).thenReturn(true);

        DuplicateResourceException error = assertThrows(DuplicateResourceException.class,
                () -> userService.create(user("ana@x.com")));

        assertEquals("Já existe um usuário com este e-mail.", error.getMessage());
        verify(users, never()).save(any());
        verify(logs, never()).save(any());
    }

    @Test
    void editingAUserOnlyChecksTheEmailAgainstOtherRecords() {
        User existing = User.builder().id(1).userName("Ana").email("ana@x.com").build();
        when(users.findById(1)).thenReturn(Optional.of(existing));

        userService.update(1, user("ana@x.com"));
        verify(users).existsByEmailIgnoreCaseAndIdNot("ana@x.com", 1);

        when(users.existsByEmailIgnoreCaseAndIdNot("bia@x.com", 1)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> userService.update(1, user("bia@x.com")));
        assertEquals("ana@x.com", existing.getEmail(), "nada é alterado");
    }

    @Test
    void creatingARecordLogsAnInsertByTheCurrentAdmin() {
        when(users.save(any(User.class))).thenReturn(User.builder().id(42).userName("Ana").build());

        userService.create(user("ana@x.com"));

        AdminLog entry = loggedEntry();
        assertEquals(AdminAction.INSERT, entry.getAction());
        assertEquals("usuarios", entry.getAffectedTable());
        assertEquals(42, entry.getRecordId());
        assertEquals(me, entry.getAdmin());
        assertNull(entry.getPreviousData());
    }

    @Test
    void editingARecordLogsAnUpdateWithTheDataBeforeTheChangeAndNoPassword() {
        User existing = User.builder().id(1).userName("Ana").email("ana@x.com").status(Status.ATIVO)
                .passwordHash("$argon2id$segredo").build();
        when(users.findById(1)).thenReturn(Optional.of(existing));

        userService.update(1, UserRequestDTO.builder().userName("Ana Maria").email("ana@x.com").status(Status.ATIVO).build());

        AdminLog entry = loggedEntry();
        assertEquals(AdminAction.UPDATE, entry.getAction());
        assertEquals(1, entry.getRecordId());
        assertTrue(entry.getPreviousData().contains("Nome: Ana\n"), entry.getPreviousData());
        assertFalse(entry.getPreviousData().contains("segredo"));
        assertEquals("Ana Maria", existing.getUserName());
    }

    @Test
    void productNeedsAnExistingCompanyAndAUniqueCasNumber() {
        ProductRequestDTO request = ProductRequestDTO.builder().companyId(5).casNumber("7732-18-5").name("Água")
                .instructions("-").dosage("-").build();

        InvalidDataException missing = assertThrows(InvalidDataException.class, () -> productService.create(request));
        assertEquals("A empresa selecionada não existe.", missing.getMessage());

        when(companies.existsById(5)).thenReturn(true);
        when(products.existsByCasNumber("7732-18-5")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> productService.create(request));
        verify(products, never()).save(any());
    }

    @Test
    void aProductHasAtMostOneDisposalSheet() {
        DisposalRequestDTO request = DisposalRequestDTO.builder().productId(3).build();
        when(products.existsById(3)).thenReturn(true);
        when(disposals.existsByProductId(3)).thenReturn(true);

        DuplicateResourceException error = assertThrows(DuplicateResourceException.class,
                () -> disposalService.create(request));

        assertEquals("Este produto já tem uma ficha de descarte.", error.getMessage());
        verify(disposals, never()).save(any());
    }

    @Test
    void anAdminCannotTakeTheirOwnAccountOutOfActive() {
        when(admins.findById(3)).thenReturn(Optional.of(me));
        AdminRequestDTO blockMyself = AdminRequestDTO.builder().userName("Eu").email("eu@quimia.com")
                .status(Status.BLOQUEADO).build();

        InvalidDataException error = assertThrows(InvalidDataException.class, () -> adminService.update(3, blockMyself));

        assertEquals("Você não pode mudar o status da sua própria conta.", error.getMessage());
        assertEquals(Status.ATIVO, me.getStatus());
    }

    @Test
    void anAdminCanChangeTheStatusOfAnotherAdmin() {
        Admin other = Admin.builder().id(8).userName("Outro").email("outro@quimia.com").status(Status.ATIVO).build();
        when(admins.findById(8)).thenReturn(Optional.of(other));

        adminService.update(8, AdminRequestDTO.builder().userName("Outro").email("outro@quimia.com")
                .status(Status.BLOQUEADO).build());

        assertEquals(Status.BLOQUEADO, other.getStatus());
    }
}
