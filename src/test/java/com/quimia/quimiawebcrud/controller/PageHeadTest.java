package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.crud.HistoryService;
import com.quimia.quimiawebcrud.service.crud.HistoryTypeService;
import com.quimia.quimiawebcrud.service.crud.ShelfService;
import com.quimia.quimiawebcrud.service.crud.UserService;
import com.quimia.quimiawebcrud.service.photo.PhotoUrlPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * O cabeçalho das telas vem de um fragmento só (fragments/head): título da aba, CSS e scripts de cada tela.
 */
@WebMvcTest({UserController.class, HistoryController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(PhotoUrlPolicy.class)
class PageHeadTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;
    @MockitoBean
    HistoryService historyService;
    @MockitoBean
    ShelfService shelfService;
    @MockitoBean
    HistoryTypeService historyTypeService;

    @Test
    void screenWithPhotosAndPasswordLoadsTheirStylesAndScripts() throws Exception {
        String html = mvc.perform(get("/users")).andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("<title>Usuários | Quimia</title>"));
        assertTrue(html.contains("/css/style.css") && html.contains("/css/detail.css") && html.contains("/css/photo.css"));
        assertTrue(html.contains("/js/password-toggle.js") && html.contains("/js/sidebar.js"));
    }

    @Test
    void readOnlyScreenSkipsWhatItDoesNotUse() throws Exception {
        String html = mvc.perform(get("/histories")).andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("<title>Relatório de usuários | Quimia</title>"));
        assertTrue(html.contains("/css/detail.css") && html.contains("/js/sidebar.js"));
        assertFalse(html.contains("/css/photo.css"), "tela sem foto não carrega o CSS de foto");
        assertFalse(html.contains("/js/password-toggle.js"), "tela sem senha não carrega o script da senha");
    }
}
