package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.PhotoUrlPolicy;
import com.quimia.quimiawebcrud.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Menu lateral: sem campo de pesquisa (não fazia nada), com os links e o botão de sair.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PhotoUrlPolicy.class)
class SidebarMenuTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;

    @Test
    void sidebarHasNoSearchFieldButKeepsTheLinksAndLogout() throws Exception {
        when(userService.readAll()).thenReturn(new ArrayList<>());

        String html = mvc.perform(get("/users")).andReturn().getResponse().getContentAsString();

        assertFalse(html.contains("sidebar-search") || html.contains("busca-menu"), "menu sem campo de pesquisa");
        assertTrue(html.contains("href=\"/admins\"") && html.contains("href=\"/users\"") && html.contains("sidebar-logout-form"));
    }
}
