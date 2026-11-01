package com.quimia.quimiawebcrud.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A troca de idioma: grava a escolha num cookie e volta para a tela de onde veio, sem nunca servir de redirecionamento
 * para outro site.
 */
@WebMvcTest(SettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SettingsControllerTest {

    @Autowired
    MockMvc mvc;

    private Cookie languageCookie(MvcResult result) {
        return result.getResponse().getCookie("quimia.lang");
    }

    @Test
    void choosingEnglishSavesTheCookieAndGoesBackToTheSameScreenAndSearch() throws Exception {
        MvcResult result = mvc.perform(post("/settings/language").param("lang", "en")
                        .header("Referer", "http://localhost/users?busca=ana&status=ATIVO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users?busca=ana&status=ATIVO"))
                .andReturn();

        Cookie cookie = languageCookie(result);
        assertNotNull(cookie);
        assertEquals("en", cookie.getValue());
        assertEquals(365 * 24 * 3600, cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("SameSite=Lax"));
    }

    @Test
    void choosingPortugueseSavesPtBr() throws Exception {
        MvcResult result = mvc.perform(post("/settings/language").param("lang", "pt-BR")
                .header("Referer", "http://localhost/")).andReturn();

        assertEquals("pt-BR", languageCookie(result).getValue());
    }

    @Test
    void aLanguageTheSystemDoesNotOfferBecomesPortuguese() throws Exception {
        for (String lang : new String[]{"fr", "", "EN-US;q=1", "<script>", "../x"}) {
            MvcResult result = mvc.perform(post("/settings/language").param("lang", lang)
                    .header("Referer", "http://localhost/")).andReturn();

            assertEquals("pt-BR", languageCookie(result).getValue(), "\"" + lang + "\"");
        }
    }

    @Test
    void theLanguageNameIsCaseInsensitiveButStoredAsTheOfferedTag() throws Exception {
        MvcResult result = mvc.perform(post("/settings/language").param("lang", "EN")).andReturn();

        assertEquals("en", languageCookie(result).getValue());
    }

    @Test
    void theLanguageParameterIsRequired() throws Exception {
        mvc.perform(post("/settings/language")).andExpect(status().isBadRequest());
    }

    @Test
    void underThePrefixTheCookieOnlyTravelsToThePanelAndTheRedirectKeepsThePrefix() throws Exception {
        MvcResult result = mvc.perform(post("/admin/settings/language").contextPath("/admin").param("lang", "en")
                        .header("Referer", "http://localhost/admin/companies?pagina=2"))
                .andExpect(redirectedUrl("/admin/companies?pagina=2"))
                .andReturn();

        assertNotNull(languageCookie(result));
    }

    private String backTo(String referer, String serverName, String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", contextPath + "/settings/language");
        request.setServerName(serverName);
        request.setContextPath(contextPath);
        if (referer != null) {
            request.addHeader("Referer", referer);
        }
        return SettingsController.backTo(request);
    }

    @Test
    void withoutAUsableRefererTheRedirectGoesToTheHomeScreen() {
        assertEquals("/", backTo(null, "quimia.tech", ""));
        assertEquals("/", backTo("", "quimia.tech", ""));
        assertEquals("/", backTo("   ", "quimia.tech", ""));
        assertEquals("/", backTo("nao e uma url", "quimia.tech", ""));
    }

    @Test
    void aRefererFromAnotherSiteNeverBecomesTheRedirect() {
        List<String> attempts = List.of(
                "https://evil.example/users",
                "https://quimia.tech.evil.example/users",
                "https://evil.example/https://quimia.tech/users",
                "https://quimia.tech@evil.example/users",
                "http://evil.example\\@quimia.tech/users",
                "//evil.example/users",
                "javascript:alert(1)",
                "data:text/html,x",
                "ftp://quimia.tech/users");
        for (String referer : attempts) {
            assertEquals("/", backTo(referer, "quimia.tech", ""), referer);
        }
    }

    @Test
    void aSamePanelRefererKeepsOnlyThePathAndTheQuery() {
        assertEquals("/users", backTo("https://quimia.tech/users", "quimia.tech", ""));
        assertEquals("/users?busca=a%20b", backTo("https://quimia.tech/users?busca=a%20b", "QUIMIA.tech", ""));
        assertEquals("/users?x=1", backTo("https://quimia.tech/users?x=1#topo", "quimia.tech", ""), "o #fragmento não vai");
        assertEquals("/companies", backTo("https://quimia.tech:8443/admin/companies", "quimia.tech", "/admin"));
    }

    @Test
    void theRedirectNeverStartsWithTwoSlashesNorTouchesOtherPrefixes() {
        assertEquals("/", backTo("https://quimia.tech//evil.example", "quimia.tech", ""));
        assertEquals("/", backTo("https://quimia.tech/admin/", "quimia.tech", "/admin"), "só o prefixo: a tela inicial");
        assertEquals("/", backTo("https://quimia.tech/outro/users", "quimia.tech", "/admin"));
        assertEquals("/", backTo("https://quimia.tech/administrador/users", "quimia.tech", "/admin"));
    }

    @Test
    void braceCharactersInTheRefererCannotBreakTheRedirect() {
        assertEquals("/", backTo("https://quimia.tech/users?a={b}", "quimia.tech", ""));
    }
}
