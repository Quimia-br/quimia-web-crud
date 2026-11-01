package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.service.crud.AdminService;
import com.quimia.quimiawebcrud.service.crud.CompanyService;
import com.quimia.quimiawebcrud.service.crud.ProductService;
import com.quimia.quimiawebcrud.service.crud.UserService;
import com.quimia.quimiawebcrud.service.photo.PhotoUrlPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Os formulários repetem as regras do servidor em atributos HTML, para o navegador avisar na hora,
 * antes de enviar. Aqui conferimos que os atributos estão presentes.
 */
@WebMvcTest({UserController.class, AdminController.class, CompanyController.class, ProductController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(PhotoUrlPolicy.class)
class FormAttributesTest {

    private static final String EMAIL_PATTERN = "pattern=\"[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}\"";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;
    @MockitoBean
    AdminService adminService;
    @MockitoBean
    CompanyService companyService;
    @MockitoBean
    ProductService productService;

    private String page(String path) throws Exception {
        when(userService.readAll()).thenReturn(new ArrayList<>());
        when(adminService.readAll()).thenReturn(new ArrayList<>());
        when(companyService.readAll()).thenReturn(new ArrayList<>());
        when(productService.readAll()).thenReturn(new ArrayList<>());
        return mvc.perform(get(path)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void emailPasswordAndBirthDateFieldsValidateInTheBrowser() throws Exception {
        for (String path : new String[]{"/users", "/admins", "/companies"}) {
            String html = page(path);
            assertTrue(html.contains(EMAIL_PATTERN), path + ": e-mail com domínio");
            assertTrue(html.contains("minlength=\"8\" maxlength=\"72\""), path + ": senha de 8 a 72 caracteres");
        }
        assertTrue(page("/users").contains("max=\"" + LocalDate.now() + "\""), "nascimento não pode ser no futuro");
    }

    @Test
    void cnpjAndCasFieldsValidateInTheBrowser() throws Exception {
        String companies = page("/companies");
        assertTrue(companies.contains("pattern=\"\\d{14}\"") && companies.contains("inputmode=\"numeric\""));
        assertTrue(page("/products").contains("pattern=\"\\d{2,7}-\\d{2}-\\d\""));
    }
}
