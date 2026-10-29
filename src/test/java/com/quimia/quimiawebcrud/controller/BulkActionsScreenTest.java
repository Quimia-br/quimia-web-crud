package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.CompanyService;
import com.quimia.quimiawebcrud.service.HistoryService;
import com.quimia.quimiawebcrud.service.HistoryTypeService;
import com.quimia.quimiawebcrud.service.PhotoUrlPolicy;
import com.quimia.quimiawebcrud.service.ProductService;
import com.quimia.quimiawebcrud.service.ShelfService;
import com.quimia.quimiawebcrud.service.UserService;
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
 * Os botões "Atualizar por..." e "Apagar por..." e os popups deles aparecem nas telas de cadastro, só com as colunas
 * que cada tela oferece; as telas só de consulta continuam sem nenhum dos dois.
 */
@WebMvcTest({UserController.class, ProductController.class, HistoryTypeController.class, HistoryController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(PhotoUrlPolicy.class)
class BulkActionsScreenTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;
    @MockitoBean
    ProductService productService;
    @MockitoBean
    CompanyService companyService;
    @MockitoBean
    HistoryTypeService historyTypeService;
    @MockitoBean
    HistoryService historyService;
    @MockitoBean
    ShelfService shelfService;

    private String page(String path) throws Exception {
        return mvc.perform(get(path)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void userScreenOffersBothPopupsWithTheirColumns() throws Exception {
        String html = page("/users");

        assertTrue(html.contains("popovertarget=\"atualizar-por\""), "botão Atualizar por...");
        assertTrue(html.contains("popovertarget=\"apagar-por\""), "botão Apagar por...");
        assertTrue(html.contains("action=\"/users/update-by\""));
        assertTrue(html.contains("action=\"/users/delete-by\""));
        assertTrue(html.contains("value=\"email\""), "e-mail serve de filtro");
        assertTrue(html.contains("Status (ATIVO, INATIVO, PENDENTE, BLOQUEADO, DESATIVADO)"), "status mostra os valores aceitos");
        assertFalse(html.contains("senha_hash"), "a senha nunca é oferecida");
    }

    @Test
    void onlyTheUpdatableColumnsCanReceiveANewValue() throws Exception {
        String html = page("/users");
        String changeSelect = html.substring(html.indexOf("name=\"changeColumn\""));
        changeSelect = changeSelect.substring(0, changeSelect.indexOf("</select>"));

        assertTrue(changeSelect.contains("value=\"status\""));
        assertFalse(changeSelect.contains("value=\"email\""), "e-mail é único: não se altera em lote");
        assertFalse(changeSelect.contains("value=\"nome\""), "o mesmo nome em vários usuários não faz sentido");
    }

    @Test
    void screenWithNothingToUpdateOnlyOffersDelete() throws Exception {
        String html = page("/history-types");

        assertFalse(html.contains("atualizar-por"), "tipo de histórico só tem o nome, que é único");
        assertTrue(html.contains("popovertarget=\"apagar-por\""));
        assertTrue(html.contains("action=\"/history-types/delete-by\""));
    }

    @Test
    void readOnlyScreensDoNotOfferEither() throws Exception {
        String html = page("/histories");

        assertFalse(html.contains("atualizar-por"));
        assertFalse(html.contains("apagar-por"));
    }

    @Test
    void productScreenAllowsMovingProductsToAnotherCompanyButNotChangingTheCas() throws Exception {
        String html = page("/products");
        String changeSelect = html.substring(html.indexOf("name=\"changeColumn\""));
        changeSelect = changeSelect.substring(0, changeSelect.indexOf("</select>"));

        assertTrue(changeSelect.contains("value=\"id_usuario_empresa\""));
        assertTrue(changeSelect.contains("value=\"marca\""));
        assertFalse(changeSelect.contains("value=\"cas_number\""), "o número CAS é único");
    }
}
