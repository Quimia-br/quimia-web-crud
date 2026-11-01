package com.quimia.quimiawebcrud.security;

import com.quimia.quimiawebcrud.controller.AuthController;
import com.quimia.quimiawebcrud.controller.HealthController;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Com a segurança real ligada e um segredo de origem configurado: sem o cabeçalho do proxy nem a tela de login
 * abre; com ele, vale o login de sempre; e o /health responde para o Render sem nenhum dos dois.
 */
@WebMvcTest({AuthController.class, HealthController.class, OriginLockTest.PanelStub.class})
@Import({SecurityConfig.class, AdminUserDetailsService.class, LoginAttemptService.class, PasswordHasher.class,
        OriginLockTest.PanelStub.class})
@TestPropertySource(properties = "app.origin-secret=segredo-do-teste")
class OriginLockTest {

    private static final String HEADER = OriginSecretFilter.HEADER;

    @Controller
    static class PanelStub {
        @GetMapping("/admins")
        @ResponseBody
        String panel() {
            return "painel";
        }
    }

    @Autowired
    MockMvc mvc;
    @MockitoBean
    AdminRepository adminRepository;

    @Test
    void withoutTheProxyHeaderEvenTheLoginPageIsRefused() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isForbidden());
    }

    @Test
    void withTheRightHeaderTheLoginPageOpens() throws Exception {
        mvc.perform(get("/login").header(HEADER, "segredo-do-teste")).andExpect(status().isOk());
    }

    @Test
    void withAWrongHeaderTheProtectedPageIsRefusedInsteadOfAskingForLogin() throws Exception {
        mvc.perform(get("/admins").header(HEADER, "errado")).andExpect(status().isForbidden());
    }

    @Test
    void withTheRightHeaderTheLoginStillProtectsThePanel() throws Exception {
        mvc.perform(get("/admins").header(HEADER, "segredo-do-teste"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void theHealthCheckAnswersOkWithNoHeaderAndNoLogin() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(content().string("ok"));
    }
}
