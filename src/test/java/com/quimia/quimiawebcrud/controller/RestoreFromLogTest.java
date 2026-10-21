package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.service.AdminLogService;
import com.quimia.quimiawebcrud.service.AdminService;
import com.quimia.quimiawebcrud.service.PhotoUrlPolicy;
import com.quimia.quimiawebcrud.service.RecordRestoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * A seta de restaurar aparece só nas exclusões que ainda podem ser desfeitas, e a rota mostra mensagens claras.
 */
@WebMvcTest(AdminLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PhotoUrlPolicy.class)
class RestoreFromLogTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminLogService adminLogService;
    @MockitoBean
    AdminService adminService;
    @MockitoBean
    RecordRestoreService restoreService;

    private static AdminLogResponseDTO log(int id, AdminAction action) {
        return AdminLogResponseDTO.builder().id(id).adminId(1).affectedTable("usuarios").recordId(7).action(action)
                .editDate(LocalDateTime.of(2026, 10, 21, 10, 0)).previousData("Nome: Ana").build();
    }

    @Test
    void theArrowAppearsOnlyOnRestorableDeletions() throws Exception {
        when(adminService.readAll()).thenReturn(new ArrayList<>());
        when(adminLogService.readAllNewestFirst()).thenReturn(new ArrayList<>(List.of(
                log(5, AdminAction.DELETE), log(4, AdminAction.INSERT), log(3, AdminAction.DELETE))));
        when(adminLogService.restorableIds()).thenReturn(Set.of(5));

        String html = mvc.perform(get("/admin-logs")).andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("restaurar-admin-log-5"), "a exclusão mais recente tem a seta");
        assertFalse(html.contains("restaurar-admin-log-3") || html.contains("restaurar-admin-log-4"));
        assertTrue(html.contains("/admin-logs/5/restore"), "o popup envia para a rota de restaurar");
    }

    @Test
    void restoringShowsASuccessMessageAndGoesBackToTheLog() throws Exception {
        mvc.perform(post("/admin-logs/5/restore"))
                .andExpect(redirectedUrl("/admin-logs"))
                .andExpect(flash().attribute("message", "Registro restaurado com sucesso."));

        verify(restoreService).restore(5);
    }

    @Test
    void businessRefusalsBecomeTheMessageOnTheScreen() throws Exception {
        doThrow(new InvalidDataException("Este registro já foi restaurado ou não pode ser restaurado."))
                .when(restoreService).restore(6);

        mvc.perform(post("/admin-logs/6/restore"))
                .andExpect(redirectedUrl("/admin-logs"))
                .andExpect(flash().attribute("error", "Este registro já foi restaurado ou não pode ser restaurado."));
    }

    @Test
    void aDatabaseConflictShowsAFriendlyMessageWithoutTechnicalDetails() throws Exception {
        doThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"usuarios_email_key\""))
                .when(restoreService).restore(7);

        var result = mvc.perform(post("/admin-logs/7/restore")).andExpect(redirectedUrl("/admin-logs")).andReturn();

        String error = String.valueOf(result.getFlashMap().get("error"));
        assertTrue(error.startsWith("Não foi possível restaurar"), error);
        assertFalse(error.contains("usuarios_email_key") || error.contains("constraint"), "sem detalhes do banco");
        assertEquals(1, result.getFlashMap().size());
    }
}
