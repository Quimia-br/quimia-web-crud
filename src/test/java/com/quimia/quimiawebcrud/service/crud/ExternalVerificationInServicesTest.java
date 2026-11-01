package com.quimia.quimiawebcrud.service.crud;

import com.quimia.quimiawebcrud.dto.AdminRequestDTO;
import com.quimia.quimiawebcrud.dto.CompanyRequestDTO;
import com.quimia.quimiawebcrud.dto.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.UserRequestDTO;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.mapper.AdminMapper;
import com.quimia.quimiawebcrud.mapper.CompanyMapper;
import com.quimia.quimiawebcrud.mapper.ProductMapper;
import com.quimia.quimiawebcrud.mapper.UserMapper;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import com.quimia.quimiawebcrud.repository.CompanyRepository;
import com.quimia.quimiawebcrud.repository.ProductRepository;
import com.quimia.quimiawebcrud.repository.UserRepository;
import com.quimia.quimiawebcrud.security.PasswordHasher;
import com.quimia.quimiawebcrud.service.CurrentAdmin;
import com.quimia.quimiawebcrud.service.EditLog;
import com.quimia.quimiawebcrud.service.photo.PhotoStorageService;
import com.quimia.quimiawebcrud.service.removal.RecordRemovalService;
import com.quimia.quimiawebcrud.service.verification.ExternalChecks;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Cada tela passa à conferência externa o valor novo e o que o registro já tinha, para só conferir o que mudou:
 * empresa (e-mail e CNPJ), usuário e administrador (e-mail) e produto (número CAS).
 */
class ExternalVerificationInServicesTest {

    private final ExternalChecks checks = mock(ExternalChecks.class);
    private final EditLog editLog = mock(EditLog.class);
    private final RecordRemovalService removal = mock(RecordRemovalService.class);

    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final CompanyService companyService = new CompanyService(companies, new CompanyMapper(), new PasswordHasher(),
            mock(PhotoStorageService.class), removal, editLog, checks);

    private final UserRepository users = mock(UserRepository.class);
    private final UserService userService = new UserService(users, new UserMapper(), new PasswordHasher(),
            mock(PhotoStorageService.class), removal, editLog, checks);

    private final AdminRepository admins = mock(AdminRepository.class);
    private final AdminService adminService = new AdminService(admins, new AdminMapper(), new PasswordHasher(), removal,
            editLog, mock(CurrentAdmin.class), checks);

    private final ProductRepository products = mock(ProductRepository.class);
    private final ProductService productService = new ProductService(products, new ProductMapper(),
            mock(CompanyRepository.class), removal, editLog, checks);

    @Test
    void aNewCompanyChecksTheEmailAndTheCnpjAgainstNothing() {
        companyService.verifyBeforeSave(CompanyRequestDTO.builder().email("c@x.com").cnpj("11222333000181").build(), null);

        verify(checks).requireEmailDomain("c@x.com", null);
        verify(checks).requireCnpj("11222333000181", null);
    }

    @Test
    void anEditedCompanyChecksAgainstWhatItAlreadyHas() {
        when(companies.findById(5)).thenReturn(Optional.of(Company.builder().id(5).email("velho@x.com").cnpj("11222333000181").build()));

        companyService.verifyBeforeSave(CompanyRequestDTO.builder().email("novo@x.com").cnpj("11222333000181").build(), 5);

        verify(checks).requireEmailDomain("novo@x.com", "velho@x.com");
        verify(checks).requireCnpj("11222333000181", "11222333000181");
    }

    @Test
    void aNewUserAndAnEditedUserChecksTheEmail() {
        userService.verifyBeforeSave(UserRequestDTO.builder().email("u@x.com").build(), null);
        verify(checks).requireEmailDomain("u@x.com", null);

        when(users.findById(2)).thenReturn(Optional.of(User.builder().id(2).email("antigo@x.com").build()));
        userService.verifyBeforeSave(UserRequestDTO.builder().email("u@x.com").build(), 2);
        verify(checks).requireEmailDomain("u@x.com", "antigo@x.com");
    }

    @Test
    void aNewAdminAndAnEditedAdminChecksTheEmail() {
        adminService.verifyBeforeSave(AdminRequestDTO.builder().email("a@x.com").build(), null);
        verify(checks).requireEmailDomain("a@x.com", null);

        when(admins.findById(3)).thenReturn(Optional.of(Admin.builder().id(3).email("antigo@x.com").build()));
        adminService.verifyBeforeSave(AdminRequestDTO.builder().email("a@x.com").build(), 3);
        verify(checks).requireEmailDomain("a@x.com", "antigo@x.com");
    }

    @Test
    void aNewProductAndAnEditedProductChecksTheCas() {
        productService.verifyBeforeSave(ProductRequestDTO.builder().casNumber("7732-18-5").build(), null);
        verify(checks).requireCas("7732-18-5", null);

        when(products.findById(9)).thenReturn(Optional.of(Product.builder().id(9).casNumber("64-17-5").build()));
        productService.verifyBeforeSave(ProductRequestDTO.builder().casNumber("7732-18-5").build(), 9);
        verify(checks).requireCas("7732-18-5", "64-17-5");
        verifyNoMoreInteractions(checks);
    }

    @Test
    void anEditOfARecordThatNoLongerExistsStillChecksAndLeavesTheErrorToTheUpdate() {
        when(users.findById(404)).thenReturn(Optional.empty());

        userService.verifyBeforeSave(UserRequestDTO.builder().email("u@x.com").build(), 404);

        verify(checks).requireEmailDomain(anyString(), isNull());
    }

    @Test
    void aRefusedCheckStopsTheSaveWithItsMessage() {
        doThrow(new InvalidDataException("Este número CAS não foi encontrado no PubChem. Confira os números."))
                .when(checks).requireCas("1234-56-6", null);

        InvalidDataException error = assertThrows(InvalidDataException.class,
                () -> productService.verifyBeforeSave(ProductRequestDTO.builder().casNumber("1234-56-6").build(), null));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("PubChem"));
    }
}
