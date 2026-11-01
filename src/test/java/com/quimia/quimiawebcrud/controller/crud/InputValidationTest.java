package com.quimia.quimiawebcrud.controller.crud;

import com.quimia.quimiawebcrud.dto.request.CompanyRequestDTO;
import com.quimia.quimiawebcrud.dto.request.ProductRequestDTO;
import com.quimia.quimiawebcrud.dto.request.UserRequestDTO;
import com.quimia.quimiawebcrud.service.crud.AdminService;
import com.quimia.quimiawebcrud.service.crud.CompanyService;
import com.quimia.quimiawebcrud.service.crud.DisposalService;
import com.quimia.quimiawebcrud.service.crud.HistoryTypeService;
import com.quimia.quimiawebcrud.service.crud.ProductService;
import com.quimia.quimiawebcrud.service.crud.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Validação dos dados enviados pelos popups de cadastro: dado inválido vira aviso na tela e nada chega ao service.
 */
@WebMvcTest({UserController.class, AdminController.class, CompanyController.class, ProductController.class,
        DisposalController.class, HistoryTypeController.class})
@AutoConfigureMockMvc(addFilters = false)
class InputValidationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;
    @MockitoBean
    AdminService adminService;
    @MockitoBean
    CompanyService companyService;
    @MockitoBean
    ProductService productService;
    @MockitoBean
    DisposalService disposalService;
    @MockitoBean
    HistoryTypeService historyTypeService;

    private static ResultMatcher errorContains(String text) {
        return result -> {
            Object error = result.getFlashMap().get("error");
            if (error == null || !error.toString().contains(text)) {
                throw new AssertionError("expected the error message to contain \"" + text + "\" but was: " + error);
            }
        };
    }

    @Test
    void userWithoutNameAndWithInvalidEmailIsRejected() throws Exception {
        mvc.perform(post("/users/save").param("userName", " ").param("email", "isso-nao-e-email").param("status", "ATIVO"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(errorContains("Informe o nome."))
                .andExpect(errorContains("Informe um e-mail completo, como nome@dominio.com."));
        verifyNoInteractions(userService);
    }

    @Test
    void emailNeedsADomainWithADotInUsersAdminsAndCompanies() throws Exception {
        for (String email : new String[]{"ana@x", "ana@gmail", "ana@gmail.c", "ana@.com", "a na@gmail.com"}) {
            mvc.perform(post("/users/save").param("userName", "Ana").param("email", email).param("status", "ATIVO"))
                    .andExpect(errorContains("e-mail completo"));
            mvc.perform(post("/admins/save").param("userName", "Ana").param("email", email).param("status", "ATIVO"))
                    .andExpect(errorContains("e-mail completo"));
            mvc.perform(post("/companies/save").param("cnpj", "12345678000195").param("name", "Limpa Bem")
                            .param("email", email).param("size", "MICRO").param("status", "ATIVO"))
                    .andExpect(errorContains("e-mail completo"));
        }
        verifyNoInteractions(userService, adminService, companyService);

        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana.paula@mail.empresa.com.br")
                        .param("password", "s3nha-forte").param("status", "ATIVO"))
                .andExpect(flash().attribute("message", "Registro criado com sucesso."));
    }

    @Test
    void validUserReachesTheService() throws Exception {
        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana@email.com")
                        .param("password", "s3nha-forte").param("birthDate", "2000-01-02").param("status", "ATIVO"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("message", "Registro criado com sucesso."));
        ArgumentCaptor<UserRequestDTO> request = ArgumentCaptor.forClass(UserRequestDTO.class);
        verify(userService).create(request.capture());
        assertEquals("Ana", request.getValue().getUserName());
    }

    @Test
    void statusIsRequired() throws Exception {
        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana@email.com"))
                .andExpect(errorContains("Selecione o status."));
        verifyNoInteractions(userService);
    }

    @Test
    void passwordRulesAllowEmptyWhenEditingButRejectShortOnes() throws Exception {
        mvc.perform(post("/users/save").param("id", "5").param("userName", "Ana").param("email", "ana@email.com")
                        .param("password", "").param("status", "ATIVO"))
                .andExpect(flash().attribute("message", "Registro atualizado com sucesso."));
        verify(userService).update(org.mockito.ArgumentMatchers.eq(5), any(UserRequestDTO.class));

        mvc.perform(post("/admins/save").param("userName", "Adm").param("email", "adm@quimia.com")
                        .param("password", "curta").param("status", "ATIVO"))
                .andExpect(errorContains("A senha deve ter de 8 a 72 caracteres."));
        verifyNoInteractions(adminService);
    }

    @Test
    void birthDateMustBeInThePastAndTheFormatMustBeValid() throws Exception {
        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana@email.com")
                        .param("birthDate", "2999-01-01").param("status", "ATIVO"))
                .andExpect(errorContains("A data de nascimento deve estar no passado."));
        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana@email.com")
                        .param("birthDate", "isso-nao-e-data").param("status", "ATIVO"))
                .andExpect(errorContains("O valor informado no campo \"birthDate\" é inválido."));
        verifyNoInteractions(userService);
    }

    @Test
    void companyCnpjMustHaveFourteenDigits() throws Exception {
        mvc.perform(post("/companies/save").param("cnpj", "12.345.678/0001-90").param("name", "Limpa Bem")
                        .param("email", "c@x.com").param("size", "MICRO").param("status", "ATIVO"))
                .andExpect(errorContains("O CNPJ deve ter 14 números."));
        verify(companyService, never()).create(any());

        mvc.perform(post("/companies/save").param("cnpj", "12345678000195").param("name", "Limpa Bem")
                        .param("email", "c@x.com").param("password", "s3nha-forte").param("size", "MICRO").param("status", "ATIVO"))
                .andExpect(flash().attribute("message", "Registro criado com sucesso."));
        ArgumentCaptor<CompanyRequestDTO> request = ArgumentCaptor.forClass(CompanyRequestDTO.class);
        verify(companyService).create(request.capture());
        assertEquals("12345678000195", request.getValue().getCnpj());
    }

    @Test
    void companyCnpjWithWrongCheckDigitsIsRefusedBeforeReachingTheService() throws Exception {
        mvc.perform(post("/companies/save").param("cnpj", "12345678000190").param("name", "Limpa Bem")
                        .param("email", "c@x.com").param("password", "s3nha-forte").param("size", "MICRO").param("status", "ATIVO"))
                .andExpect(errorContains("O CNPJ não é válido"));
        mvc.perform(post("/companies/save").param("cnpj", "11111111111111").param("name", "Limpa Bem")
                        .param("email", "c@x.com").param("password", "s3nha-forte").param("size", "MICRO").param("status", "ATIVO"))
                .andExpect(errorContains("O CNPJ não é válido"));
        verifyNoInteractions(companyService);
    }

    @Test
    void productCasWithWrongCheckDigitIsRefusedBeforeReachingTheService() throws Exception {
        mvc.perform(post("/products/save").param("companyId", "1").param("name", "Cloro").param("casNumber", "7681-52-8")
                        .param("instructions", "Diluir").param("dosage", "1 ml"))
                .andExpect(errorContains("O número CAS não é válido"));
        verifyNoInteractions(productService);
    }

    @Test
    void productNeedsCompanyCasInNumericFormatAndTexts() throws Exception {
        mvc.perform(post("/products/save").param("name", "Cloro").param("casNumber", "abc").param("brand", "LB")
                        .param("instructions", "Diluir").param("dosage", "1 ml"))
                .andExpect(redirectedUrl("/products"))
                .andExpect(errorContains("Selecione a empresa."))
                .andExpect(errorContains("O número CAS deve ter o formato 0000-00-0."));
        verifyNoInteractions(productService);

        mvc.perform(post("/products/save").param("companyId", "1").param("name", "Cloro").param("casNumber", "7681-52-9")
                        .param("instructions", "Diluir").param("dosage", "1 ml"))
                .andExpect(flash().attribute("message", "Registro criado com sucesso."));
        ArgumentCaptor<ProductRequestDTO> request = ArgumentCaptor.forClass(ProductRequestDTO.class);
        verify(productService).create(request.capture());
        assertEquals("7681-52-9", request.getValue().getCasNumber());
    }

    @Test
    void disposalNeedsAProductAndHistoryTypeNeedsAName() throws Exception {
        mvc.perform(post("/disposals/save").param("productDisposalMethod", "Diluir"))
                .andExpect(errorContains("Selecione o produto."));
        verifyNoInteractions(disposalService);

        mvc.perform(post("/history-types/save").param("name", ""))
                .andExpect(errorContains("Informe o nome."));
        verifyNoInteractions(historyTypeService);
    }

    @Test
    void shownMessagesAreLimitedToThree() throws Exception {
        mvc.perform(post("/products/save"))
                .andExpect(result -> {
                    String error = result.getFlashMap().get("error").toString();
                    assertEquals(3, error.split("\\. ").length, "three sentences at most: " + error);
                });
    }
}
