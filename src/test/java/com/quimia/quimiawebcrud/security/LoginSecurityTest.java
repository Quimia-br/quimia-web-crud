package com.quimia.quimiawebcrud.security;

import com.quimia.quimiawebcrud.controller.AuthController;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login de administrador, proteção das rotas, CSRF, logout, bloqueio por tentativas e cabeçalhos de segurança.
 */
@WebMvcTest({AuthController.class, LoginSecurityTest.PanelStub.class})
@Import({SecurityConfig.class, AdminUserDetailsService.class, LoginAttemptService.class, PasswordHasher.class,
        LoginSecurityTest.PanelStub.class})
class LoginSecurityTest {

    /** Páginas de mentira só para ter o que proteger. */
    @Controller
    static class PanelStub {
        @GetMapping("/admins")
        @ResponseBody
        String panel() {
            return "painel";
        }

        @PostMapping("/admins/save")
        @ResponseBody
        String save() {
            return "salvo";
        }

        @GetMapping("/uploads/photos/x.png")
        @ResponseBody
        String photo() {
            return "foto";
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    PasswordHasher hasher;
    @MockitoBean
    AdminRepository adminRepository;

    private Admin admin(String email, Status status) {
        return Admin.builder().id(1).userName("Adm").email(email).status(status).passwordHash(hasher.hash("Segredo#1")).build();
    }

    @BeforeEach
    void admins() {
        // cada teste usa uma conta diferente, porque o bloqueio por tentativas guarda estado entre os testes
        when(adminRepository.findByEmailIgnoreCase(anyString())).thenAnswer(invocation -> {
            String email = ((String) invocation.getArgument(0)).toLowerCase();
            return switch (email) {
                case "adm@quimia.com", "adm2@quimia.com", "lock@quimia.com" -> Optional.of(admin(email, Status.ATIVO));
                case "off@quimia.com" -> Optional.of(admin(email, Status.INATIVO));
                default -> Optional.empty();
            };
        });
    }

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(formLogin("/login").userParameter("email").passwordParam("password")
                .user(email).password(password)).andReturn();
    }

    @Test
    void protectedRoutesRedirectToTheLoginPage() throws Exception {
        mvc.perform(get("/admins")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/qualquer-outra-rota")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/admins/save").with(csrf())).andExpect(status().is3xxRedirection());
    }

    @Test
    void loginPhotosAndVisualFilesStayOpen() throws Exception {
        String page = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(page.contains("name=\"email\"") && page.contains("name=\"password\""));
        assertTrue(page.contains("name=\"_csrf\""), "o formulário traz o token CSRF");
        assertFalse(page.contains("Cadastre-se"), "não existe cadastro público de administrador");
        mvc.perform(get("/css/style.css")).andExpect(status().isOk());
        mvc.perform(get("/icons/logo.svg")).andExpect(status().isOk());
        mvc.perform(get("/uploads/photos/x.png")).andExpect(status().isOk());
    }

    @Test
    void loginLogoutAndCsrf() throws Exception {
        MvcResult ok = login("ADM@Quimia.com", "Segredo#1");   // e-mail sem diferenciar maiúsculas
        String landing = ok.getResponse().getRedirectedUrl();
        assertTrue("/".equals(landing) || "/admins".equals(landing), landing);
        MockHttpSession session = (MockHttpSession) ok.getRequest().getSession(false);
        assertNotNull(session);

        mvc.perform(get("/admins").session(session)).andExpect(status().isOk());
        mvc.perform(post("/admins/save").session(session)).andExpect(status().isForbidden());   // sem token CSRF
        mvc.perform(post("/admins/save").session(session).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/logout").session(session));                                             // GET não desloga
        mvc.perform(get("/admins").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/admins").session(session)).andExpect(status().is3xxRedirection());
    }

    @Test
    void everyFailureLooksTheSame() throws Exception {
        for (String[] attempt : new String[][]{{"adm2@quimia.com", "errada"}, {"naoexiste@x.com", "Segredo#1"},
                {"off@quimia.com", "Segredo#1"}, {"", ""}}) {
            assertEquals("/login?error", login(attempt[0], attempt[1]).getResponse().getRedirectedUrl(), attempt[0]);
        }
        mvc.perform(post("/login").param("email", "adm@quimia.com").param("password", "Segredo#1"))
                .andExpect(status().isForbidden());   // sem CSRF
    }

    @Test
    void accountIsLockedAfterFiveWrongPasswords() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("lock@quimia.com", "errada" + i);
        }
        assertEquals("/login?error", login("lock@quimia.com", "Segredo#1").getResponse().getRedirectedUrl(),
                "bloqueada mesmo com a senha certa");
        String landing = login("adm@quimia.com", "Segredo#1").getResponse().getRedirectedUrl();
        assertTrue("/".equals(landing) || "/admins".equals(landing), "outras contas não são afetadas");
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        var response = mvc.perform(get("/login")).andReturn().getResponse();
        String csp = response.getHeader("Content-Security-Policy");
        assertNotNull(csp);
        assertTrue(csp.contains("default-src 'self'") && csp.contains("script-src 'self'")
                && csp.contains("frame-ancestors 'none'") && csp.contains("object-src 'none'"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("same-origin", response.getHeader("Referrer-Policy"));
    }

    @Test
    void loginMessagesAppearOnlyWhenNeeded() throws Exception {
        String hidden = "(?s).*<p class=\"auth-message auth-message-error\"[^>]*hidden[^>]*>E-mail ou senha.*";
        String plain = mvc.perform(get("/login")).andReturn().getResponse().getContentAsString();
        String failed = mvc.perform(get("/login").param("error", "")).andReturn().getResponse().getContentAsString();

        assertTrue(plain.matches(hidden), "sem erro a mensagem fica escondida");
        assertFalse(failed.matches(hidden), "depois de uma falha a mensagem aparece");
        assertTrue(failed.contains("E-mail ou senha inv"));
    }
}
