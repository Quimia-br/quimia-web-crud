package com.quimia.quimiawebcrud.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A tela de login nos dois idiomas, com o tema escolhido antes de a página aparecer.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginPageLanguageTest {

    @Autowired
    MockMvc mvc;

    private String login(String language, String query) throws Exception {
        var request = get("/login" + query);
        if (language != null) {
            request = request.cookie(new Cookie("quimia.lang", language));
        }
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void theLoginIsInPortugueseByDefault() throws Exception {
        String html = login(null, "");

        assertTrue(html.contains("lang=\"pt-BR\"") && html.contains("<title>Entrar | Quimia</title>"));
        assertTrue(html.contains("Bem-vindo!") && html.contains("Acessar conta") && html.contains("placeholder=\"Senha\""));
        assertTrue(html.contains("data-label-show=\"Mostrar senha\"") && html.contains("data-label-hide=\"Ocultar senha\""));
    }

    @Test
    void theLoginInEnglishTranslatesTheFormAndTheMessages() throws Exception {
        String html = login("en", "?error");

        assertTrue(html.contains("lang=\"en\"") && html.contains("<title>Log in | Quimia</title>"));
        assertTrue(html.contains("Welcome!") && html.contains("Access your account") && html.contains("placeholder=\"Password\""));
        assertTrue(html.contains("Invalid e-mail or password.") && html.contains(">Log in</button>"));
        assertTrue(html.contains("data-label-show=\"Show password\"") && html.contains("data-label-hide=\"Hide password\""));
        assertFalse(html.contains("Bem-vindo") || html.contains("Senha") || html.contains("Acessar conta"));
    }

    @Test
    void theBlockedAndLoggedOutNoticesAreTranslatedToo() throws Exception {
        assertTrue(login("en", "?blocked").contains("Too many attempts. Wait a few minutes and try again."));
        assertTrue(login("en", "?logout").contains("You have logged out of the panel."));
        assertTrue(login(null, "?blocked").contains("Muitas tentativas. Aguarde alguns minutos e tente de novo."));
    }

    @Test
    void theLoginAndTheLogoutNoticeNeverLoadTheThemeScriptSoTheyStayLight() throws Exception {
        assertFalse(login(null, "").contains("/js/theme.js"));
        assertFalse(login(null, "?logout").contains("/js/theme.js"));
    }

    @Test
    void theLoginAndTheLogoutNoticeOfferABackLinkToTheLandingPageInBothLanguages() throws Exception {
        for (String query : new String[] {"", "?logout"}) {
            String pt = login(null, query);
            assertTrue(pt.contains("class=\"auth-back\" href=\"https://quimia.tech\"") && pt.contains("Voltar ao site"));
            assertTrue(login("en", query).contains("Back to the website"));
        }
    }
}
