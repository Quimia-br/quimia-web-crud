package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.UserRequestDTO;
import com.quimia.quimiawebcrud.exception.DuplicateResourceException;
import com.quimia.quimiawebcrud.exception.GlobalExceptionHandler;
import com.quimia.quimiawebcrud.exception.ResourceNotFoundException;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
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
        mvc.perform(post("/users/save").param("id", "7").param("userName", "Ana"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("message", "Registro atualizado com sucesso."));
        verify(userService).update(org.mockito.ArgumentMatchers.eq(7), any(UserRequestDTO.class));

        mvc.perform(post("/users/7/delete"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("message", "Registro excluído com sucesso."));
        verify(userService).delete(7);
    }

    @Test
    void businessErrorsBecomeTheMessageOnTheScreen() throws Exception {
        when(userService.create(any())).thenThrow(new DuplicateResourceException("O e-mail já está cadastrado."));
        doThrow(new ResourceNotFoundException("Registro não encontrado com o ID: 9")).when(userService).delete(9);

        mvc.perform(post("/users/save").param("userName", "Ana"))
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

        mvc.perform(post("/users/save").param("userName", "Ana"))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("error",
                        "Não foi possível salvar: já existe um registro com esses dados ou algum dado relacionado é inválido."));
    }

    @Test
    void unexpectedErrorsGoToTheErrorPageInsteadOfLeakingTheirMessage() throws Exception {
        when(userService.create(any())).thenThrow(new IllegalStateException("detalhe interno"));

        mvc.perform(post("/users/save").param("userName", "Ana"))
                .andExpect(status().isInternalServerError());
    }
}
