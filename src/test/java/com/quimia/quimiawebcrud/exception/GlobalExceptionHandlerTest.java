package com.quimia.quimiawebcrud.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.FailingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.FailingController.class})
class GlobalExceptionHandlerTest {

    @Controller
    static class FailingController {
        @GetMapping("/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Registro não encontrado com o ID: 7");
        }

        @GetMapping("/duplicate")
        String duplicate() {
            throw new DuplicateResourceException("O produto 1 já está na estante 2.");
        }

        @GetMapping("/invalid")
        String invalid() {
            throw new InvalidDataException("A senha é obrigatória.");
        }

        @GetMapping("/in-use")
        String inUse() {
            throw new DataIntegrityViolationException("violates foreign key constraint fk_secreta");
        }

        @GetMapping("/photo")
        String photo() {
            throw new PhotoException("Formato de imagem não aceito. Envie uma foto JPEG ou PNG.");
        }

        @GetMapping("/too-large")
        String tooLarge() {
            throw new MaxUploadSizeExceededException(5 * 1024 * 1024);
        }

        @GetMapping("/db-down")
        String dbDown() {
            throw new DataAccessResourceFailureException("Connection refused: host=10.0.0.5 user=secreto");
        }

        @GetMapping("/db-busy")
        String dbBusy() {
            throw new CannotAcquireLockException("deadlock detected on tabela_secreta");
        }

        @GetMapping("/no-connection")
        String noConnection() {
            throw new CannotCreateTransactionException("HikariPool-1 - Connection is not available");
        }

        @GetMapping("/dashboard")
        String dashboard() {
            throw new DashboardUnavailableException("Não foi possível carregar o dashboard agora.");
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("detalhe interno secreto");
        }

        @GetMapping("/number/{id}")
        String number(@PathVariable Integer id) {
            return "error";
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void notFoundShowsTheMessageWithStatus404() throws Exception {
        mvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Registro não encontrado com o ID: 7")));
    }

    @Test
    void duplicateAndInvalidDataUseTheirOwnStatus() throws Exception {
        mvc.perform(get("/duplicate")).andExpect(status().isConflict())
                .andExpect(content().string(containsString("já está na estante")));
        mvc.perform(get("/invalid")).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("A senha é obrigatória.")));
    }

    @Test
    void databaseErrorsNeverExposeTheTechnicalMessage() throws Exception {
        mvc.perform(get("/in-use"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("fk_secreta"))))
                .andExpect(content().string(containsString("está em uso")));
    }

    @Test
    void databaseUnavailableOrBusyAsksToTryAgainWith503AndHidesTheDetail() throws Exception {
        for (String path : new String[]{"/db-down", "/db-busy", "/no-connection"}) {
            mvc.perform(get(path))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string(containsString("Tente novamente em instantes.")))
                    .andExpect(content().string(not(containsString("secreto"))))
                    .andExpect(content().string(not(containsString("Hikari"))));
        }
    }

    @Test
    void dashboardUnavailableUses503InsteadOfTheGenericBusinessStatus() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("Não foi possível carregar o dashboard agora.")));
    }

    @Test
    void otherBusinessErrorsShowTheirMessageInsteadOfAnUnexpectedError() throws Exception {
        mvc.perform(get("/photo"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Envie uma foto JPEG ou PNG.")));
    }

    @Test
    void fileAboveTheUploadLimitExplainsTheMaximumSize() throws Exception {
        mvc.perform(get("/too-large"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().string(containsString("no máximo 2 MB")));
    }

    @Test
    void unexpectedErrorsShowAGenericMessageAndNeverTheInternalDetail() throws Exception {
        mvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("detalhe interno secreto"))))
                .andExpect(content().string(containsString("Ocorreu um erro inesperado")));
    }

    @Test
    void invalidUrlValueAndUnknownPageAreClientErrors() throws Exception {
        mvc.perform(get("/number/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void errorPageUsesTheNewStyleSheet() throws Exception {
        mvc.perform(get("/not-found"))
                .andExpect(content().string(containsString("/css/error.css")))
                .andExpect(content().string(not(containsString("quimia.css"))));
    }
}
