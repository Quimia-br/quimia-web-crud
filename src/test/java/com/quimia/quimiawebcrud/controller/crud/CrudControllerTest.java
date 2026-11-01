package com.quimia.quimiawebcrud.controller.crud;

import com.quimia.quimiawebcrud.dto.request.UserRequestDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.exception.GlobalExceptionHandler;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.exception.ResourceNotFoundException;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.service.crud.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rotas de salvar e excluir que todas as telas de cadastro herdam do GenericController (testadas com o UserController).
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CrudControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;

    @Test
    void saveWithoutIdCreatesAndBindsDatesEnumsAndText() throws Exception {
        mvc.perform(post("/users/save")
                        .param("userName", "Ana").param("email", "ana@email.com").param("password", "s3nha-forte")
                        .param("birthDate", "2000-01-02").param("status", "ATIVO"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("message", "Registro criado com sucesso."));

        ArgumentCaptor<UserRequestDTO> request = ArgumentCaptor.forClass(UserRequestDTO.class);
        verify(userService).create(request.capture());
        assertEquals("Ana", request.getValue().getUserName());
        assertEquals(LocalDate.of(2000, 1, 2), request.getValue().getBirthDate());
        assertEquals(Status.ATIVO, request.getValue().getStatus());
    }

    @Test
    void saveWithIdUpdatesAndDeleteRemoves() throws Exception {
        mvc.perform(post("/users/save").param("id", "7").param("userName", "Ana").param("email", "ana@email.com").param("status", "ATIVO"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("message", "Registro atualizado com sucesso."));
        verify(userService).update(org.mockito.ArgumentMatchers.eq(7), any(UserRequestDTO.class));

        mvc.perform(post("/users/7/delete"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("message", "Registro excluído com sucesso."));
        verify(userService).delete(7);
    }

    @Test
    void theExternalChecksRunBeforeCreatingAndBeforeUpdating() throws Exception {
        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana@email.com").param("status", "ATIVO"));
        mvc.perform(post("/users/save").param("id", "7").param("userName", "Ana").param("email", "ana@email.com").param("status", "ATIVO"));

        InOrder order = inOrder(userService);
        order.verify(userService).verifyBeforeSave(any(UserRequestDTO.class), isNull());
        order.verify(userService).create(any(UserRequestDTO.class));
        order.verify(userService).verifyBeforeSave(any(UserRequestDTO.class), eq(7));
        order.verify(userService).update(eq(7), any(UserRequestDTO.class));
    }

    @Test
    void aFailedExternalCheckShowsItsMessageAndNothingIsSaved() throws Exception {
        doThrow(new InvalidDataException("Este CNPJ não foi encontrado na base da Receita Federal. Confira os números."))
                .when(userService).verifyBeforeSave(any(), any());

        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana@email.com").param("status", "ATIVO"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("error", "Este CNPJ não foi encontrado na base da Receita Federal. Confira os números."));
        mvc.perform(post("/users/save").param("id", "7").param("userName", "Ana").param("email", "ana@email.com").param("status", "ATIVO"))
                .andExpect(flash().attribute("error", "Este CNPJ não foi encontrado na base da Receita Federal. Confira os números."));

        verify(userService, never()).create(any());
        verify(userService, never()).update(anyInt(), any());
    }

    @Test
    void businessErrorsBecomeTheMessageOnTheScreen() throws Exception {
        when(userService.create(any())).thenThrow(new DuplicateResourceException("O e-mail já está cadastrado."));
        doThrow(new ResourceNotFoundException("Registro não encontrado com o ID: 9")).when(userService).delete(9);

        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana@email.com").param("status", "ATIVO"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("error", "O e-mail já está cadastrado."));
        mvc.perform(post("/users/9/delete"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("error", "Registro não encontrado com o ID: 9"));
    }

    @Test
    void deletingARecordInUseShowsAFriendlyMessageWithoutDatabaseDetails() throws Exception {
        doThrow(new DataIntegrityViolationException("violates foreign key constraint \"fk_produto_empresa\"")).when(userService).delete(anyInt());

        mvc.perform(post("/users/3/delete"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("error", "Não foi possível excluir: este registro está em uso por outros dados."));
    }

    @Test
    void savingADuplicateRecordShowsAFriendlyMessage() throws Exception {
        when(userService.create(any())).thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"usuarios_email_key\""));

        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana@email.com").param("status", "ATIVO"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("error",
                        "Não foi possível salvar: já existe um registro com esses dados ou algum dado relacionado é inválido."));
    }

    @Test
    void updateByColumnChangesTheMatchingRecordsAndReportsHowMany() throws Exception {
        when(userService.updateByColumn("status", "BLOQUEADO", Map.of("status", "ATIVO"))).thenReturn(3);

        mvc.perform(post("/users/update-by")
                        .param("filterColumn", "status").param("filterValue", "BLOQUEADO")
                        .param("changeColumn", "status").param("newValue", "ATIVO"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("message", "3 registros atualizados com sucesso."));
    }

    @Test
    void updateByColumnOnlyAcceptsTheColumnsTheScreenOffers() throws Exception {
        // e-mail serve de filtro, mas não aceita novo valor; "senha_hash" nem é oferecida
        mvc.perform(post("/users/update-by")
                        .param("filterColumn", "nome").param("filterValue", "Ana")
                        .param("changeColumn", "email").param("newValue", "x@y.com"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("error", "Escolha uma das colunas da lista."));
        mvc.perform(post("/users/update-by")
                        .param("filterColumn", "senha_hash").param("filterValue", "x")
                        .param("changeColumn", "status").param("newValue", "ATIVO"))
                .andExpect(flash().attribute("error", "Escolha uma das colunas da lista."));

        verify(userService, never()).updateByColumn(any(), any(), any());
    }

    @Test
    void updateByColumnWithNoMatchOrABadValueShowsAMessage() throws Exception {
        when(userService.updateByColumn(eq("nome"), eq("Ninguém"), any())).thenReturn(0);
        when(userService.updateByColumn(eq("nome"), eq("Ana"), any()))
                .thenThrow(new InvalidDataException("O valor \"X\" não é válido para a coluna status."));

        mvc.perform(post("/users/update-by").param("filterColumn", "nome").param("filterValue", "Ninguém")
                        .param("changeColumn", "status").param("newValue", "ATIVO"))
                .andExpect(flash().attribute("error", "Nenhum registro encontrado com esse valor."));
        mvc.perform(post("/users/update-by").param("filterColumn", "nome").param("filterValue", "Ana")
                        .param("changeColumn", "status").param("newValue", "X"))
                .andExpect(flash().attribute("error", "O valor \"X\" não é válido para a coluna status."));
    }

    @Test
    void bulkRoutesRefuseValuesLongerThanTheFormAllows() throws Exception {
        String tooLongFilter = "a".repeat(256);
        String tooLongValue = "a".repeat(1001);

        mvc.perform(post("/users/update-by").param("filterColumn", "nome").param("filterValue", tooLongFilter)
                        .param("changeColumn", "status").param("newValue", "ATIVO"))
                .andExpect(flash().attribute("error", "O valor informado é muito longo."));
        mvc.perform(post("/users/update-by").param("filterColumn", "nome").param("filterValue", "Ana")
                        .param("changeColumn", "status").param("newValue", tooLongValue))
                .andExpect(flash().attribute("error", "O valor informado é muito longo."));
        mvc.perform(post("/users/delete-by").param("filterColumn", "nome").param("filterValue", tooLongFilter))
                .andExpect(flash().attribute("error", "O valor informado é muito longo."));

        verify(userService, never()).updateByColumn(any(), any(), any());
        verify(userService, never()).deleteByColumn(any(), any());
    }

    @Test
    void updateByColumnWithAValueTheDatabaseRefusesShowsAFriendlyMessage() throws Exception {
        when(userService.updateByColumn(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("value too long for type character varying(50)"));

        mvc.perform(post("/users/update-by").param("filterColumn", "nome").param("filterValue", "Ana")
                        .param("changeColumn", "status").param("newValue", "ATIVO"))
                .andExpect(flash().attribute("error",
                        "Não foi possível atualizar: o novo valor não é aceito (tamanho, tipo ou registro relacionado inexistente)."));
    }

    @Test
    void deleteByColumnRemovesTheMatchingRecordsAndShowsFriendlyErrors() throws Exception {
        when(userService.deleteByColumn("status", "INATIVO")).thenReturn(1);
        when(userService.deleteByColumn("status", "BLOQUEADO"))
                .thenThrow(new DataIntegrityViolationException("violates foreign key constraint"));

        mvc.perform(post("/users/delete-by").param("filterColumn", "status").param("filterValue", "INATIVO"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("message", "1 registro excluído com sucesso."));
        mvc.perform(post("/users/delete-by").param("filterColumn", "status").param("filterValue", "BLOQUEADO"))
                .andExpect(flash().attribute("error", "Não foi possível excluir: algum dos registros está em uso por outros dados."));
        mvc.perform(post("/users/delete-by").param("filterColumn", "senha_hash").param("filterValue", "x"))
                .andExpect(flash().attribute("error", "Escolha uma das colunas da lista."));

        verify(userService, never()).deleteByColumn(eq("senha_hash"), any());
    }

    @Test
    void unexpectedErrorsGoToTheErrorPageInsteadOfLeakingTheirMessage() throws Exception {
        when(userService.create(any())).thenThrow(new IllegalStateException("detalhe interno"));

        mvc.perform(post("/users/save").param("userName", "Ana").param("email", "ana@email.com").param("status", "ATIVO"))
                .andExpect(status().isInternalServerError());
    }
}
