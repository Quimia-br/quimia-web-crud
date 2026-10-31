package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.service.crud.AdminService;
import com.quimia.quimiawebcrud.service.crud.CompanyService;
import com.quimia.quimiawebcrud.service.crud.ProductService;
import com.quimia.quimiawebcrud.service.crud.UserService;
import com.quimia.quimiawebcrud.service.photo.PhotoUrlPolicy;
import com.quimia.quimiawebcrud.service.removal.AdminLogService;
import com.quimia.quimiawebcrud.service.removal.RecordRestoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O ícone de olho de cada linha abre um popup somente leitura com todos os dados do registro.
 */
@WebMvcTest({UserController.class, ProductController.class, AdminLogController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(PhotoUrlPolicy.class)
class RecordDetailsTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RecordRestoreService restoreService;
    @MockitoBean
    UserService userService;
    @MockitoBean
    ProductService productService;
    @MockitoBean
    CompanyService companyService;
    @MockitoBean
    AdminLogService adminLogService;
    @MockitoBean
    AdminService adminService;

    private String page(String url) throws Exception {
        return mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void eachUserRowHasAViewPopupWithItsData() throws Exception {
        when(userService.readAll()).thenReturn(new ArrayList<>(List.of(
                UserResponseDTO.builder().id(1).userName("Ana").email("ana@email.com").status(Status.ATIVO)
                        .signUpDate(LocalDateTime.of(2026, 9, 5, 8, 30)).photoUrl("https://cdn.exemplo.com/ana.jpg").build(),
                UserResponseDTO.builder().id(2).userName("Bia").email("bia@email.com").status(Status.BLOQUEADO)
                        .photoUrl("javascript:alert(1)").build())));

        String html = page("/users");

        assertTrue(html.contains("id=\"ver-user-1\"") && html.contains("id=\"ver-user-2\""));
        assertTrue(html.contains("popovertarget=\"ver-user-1\"") && html.contains("popovertarget=\"ver-user-2\""),
                "the eye button opens the popup");
        assertFalse(html.contains("href=\"#\" class=\"btn-icon\""), "no more empty view links");
        assertTrue(html.contains("ana@email.com") && html.contains("05/09/2026 08:30"));
        assertTrue(html.contains("src=\"https://cdn.exemplo.com/ana.jpg\""), "safe photo shown in the details");
        assertFalse(html.contains("javascript:alert"), "unsafe photo address never reaches the page");
        assertTrue(html.contains("<dd>—</dd>") || html.contains(">—<"), "empty values show a dash");
    }

    @Test
    void valuesAreEscapedInsideThePopup() throws Exception {
        when(userService.readAll()).thenReturn(new ArrayList<>(List.of(
                UserResponseDTO.builder().id(1).userName("<script>alert(1)</script>").email("x@x.com").status(Status.ATIVO).build())));

        String html = page("/users");

        assertFalse(html.contains("<script>alert(1)"), "HTML in a name is never executed");
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    @Test
    void productDetailsShowTheCompanyNameAndLongTexts() throws Exception {
        when(productService.readAll()).thenReturn(new ArrayList<>(List.of(
                ProductResponseDTO.builder().id(3).companyId(9).name("Cloro").casNumber("7681-52-9")
                        .instructions("Diluir em água antes de usar.").dosage("1 ml por litro").build())));
        when(companyService.readAll()).thenReturn(new ArrayList<>(List.of(CompanyResponseDTO.builder().id(9).name("Limpa Bem").build())));

        String html = page("/products");

        assertTrue(html.contains("id=\"ver-product-3\""));
        assertTrue(html.contains("Limpa Bem") && html.contains("Diluir em água antes de usar.") && html.contains("1 ml por litro"));
    }

    @Test
    void adminLogDetailsShowThePreviousDataAsPreformattedText() throws Exception {
        when(adminService.readAll()).thenReturn(new ArrayList<>(List.of(AdminResponseDTO.builder().id(1).userName("Adm").build())));
        when(adminLogService.readAllNewestFirst()).thenReturn(new ArrayList<>(List.of(
                AdminLogResponseDTO.builder().id(5).adminId(1).affectedTable("produtos").recordId(2).action(AdminAction.UPDATE)
                        .previousData("{\"nome\":\"Antigo\"}").editDate(LocalDateTime.of(2026, 10, 6, 14, 32)).build())));

        String html = page("/admin-logs");

        assertTrue(html.contains("id=\"ver-admin-log-5\""));
        assertTrue(html.contains("<pre class=\"detail-pre\""));
        assertTrue(html.contains("&quot;nome&quot;:&quot;Antigo&quot;") || html.contains("\"nome\":\"Antigo\""));
        assertTrue(html.contains("06/10/2026 14:32"));
    }
}
