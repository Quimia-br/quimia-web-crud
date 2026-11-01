package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.dto.DisposalResponseDTO;
import com.quimia.quimiawebcrud.dto.HistoryResponseDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.dto.LocationResponseDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.dto.ShelfProductResponseDTO;
import com.quimia.quimiawebcrud.dto.ShelfResponseDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.model.enums.CompanySize;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.service.crud.AdminService;
import com.quimia.quimiawebcrud.service.crud.CompanyService;
import com.quimia.quimiawebcrud.service.crud.DisposalService;
import com.quimia.quimiawebcrud.service.crud.HistoryService;
import com.quimia.quimiawebcrud.service.crud.HistoryTypeService;
import com.quimia.quimiawebcrud.service.crud.LocationService;
import com.quimia.quimiawebcrud.service.crud.ProductService;
import com.quimia.quimiawebcrud.service.crud.ShelfProductService;
import com.quimia.quimiawebcrud.service.crud.ShelfService;
import com.quimia.quimiawebcrud.service.crud.UserService;
import com.quimia.quimiawebcrud.service.photo.PhotoUrlPolicy;
import com.quimia.quimiawebcrud.service.removal.AdminLogService;
import com.quimia.quimiawebcrud.service.removal.RecordRestoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtros da barra de cada lista e paginação (10 por página, mantendo os filtros nos links).
 */
@WebMvcTest({UserController.class, AdminController.class, CompanyController.class, ProductController.class,
        DisposalController.class, ShelfController.class, HistoryController.class, HistoryTypeController.class,
        LocationController.class, AdminLogController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(PhotoUrlPolicy.class)
class ListFiltersAndPaginationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RecordRestoreService restoreService;
    @MockitoBean
    UserService userService;
    @MockitoBean
    AdminService adminService;
    @MockitoBean
    CompanyService companyService;
    @MockitoBean
    ProductService productService;
    @MockitoBean
    DisposalService disposalService;
    @MockitoBean
    ShelfService shelfService;
    @MockitoBean
    ShelfProductService shelfProductService;
    @MockitoBean
    HistoryService historyService;
    @MockitoBean
    HistoryTypeService historyTypeService;
    @MockitoBean
    LocationService locationService;
    @MockitoBean
    AdminLogService adminLogService;

    @BeforeEach
    void data() {
        List<UserResponseDTO> users = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            users.add(UserResponseDTO.builder().id(i).userName("User %02d".formatted(i)).email("u" + i + "@x.com")
                    .status(i % 5 == 0 ? Status.BLOQUEADO : Status.ATIVO).signUpDate(LocalDateTime.of(2026, 10, i, 10, 0)).build());
        }
        users.add(UserResponseDTO.builder().id(26).userName("João Pé").email("jp@x.com").status(Status.ATIVO)
                .signUpDate(LocalDateTime.of(2026, 9, 1, 8, 0)).build());
        when(userService.readAll()).thenReturn(users);
        when(adminService.readAll()).thenReturn(new ArrayList<>(List.of(AdminResponseDTO.builder().id(1).userName("Adm").status(Status.ATIVO).build())));
        when(companyService.readAll()).thenReturn(new ArrayList<>(List.of(
                CompanyResponseDTO.builder().id(1).name("Limpa Bem").cnpj("12345678000190").size(CompanySize.MICRO).status(Status.ATIVO).build(),
                CompanyResponseDTO.builder().id(2).name("Brilho").cnpj("98765432000110").size(CompanySize.GRANDE).status(Status.INATIVO).build())));
        when(productService.readAll()).thenReturn(new ArrayList<>(List.of(
                ProductResponseDTO.builder().id(1).companyId(1).casNumber("7681-52-9").name("Cloro").brand("LB").build(),
                ProductResponseDTO.builder().id(2).companyId(2).casNumber("7647-01-0").name("Acido").brand("BT").build())));
        when(disposalService.readAll()).thenReturn(new ArrayList<>(List.of(
                DisposalResponseDTO.builder().id(1).productId(1).productDisposalMethod("Diluir").build(),
                DisposalResponseDTO.builder().id(2).productId(2).productDisposalMethod("").build())));
        when(shelfService.readAll()).thenReturn(new ArrayList<>(List.of(
                ShelfResponseDTO.builder().id(1).userId(1).room("Cozinha").build(),
                ShelfResponseDTO.builder().id(2).userId(2).room("Lavanderia").build())));
        when(shelfProductService.readAll()).thenReturn(new ArrayList<>(List.of(ShelfProductResponseDTO.builder().shelfId(2).productId(1).build())));
        when(locationService.readAll()).thenReturn(new ArrayList<>(List.of(
                LocationResponseDTO.builder().id(1).userId(1).zipCode("01310100").latitude(new BigDecimal("1")).longitude(new BigDecimal("2")).build(),
                LocationResponseDTO.builder().id(2).userId(2).zipCode("20000000").build())));
        when(historyTypeService.readAll()).thenReturn(new ArrayList<>(List.of(
                HistoryTypeResponseDTO.builder().id(1).name("Uso").build(), HistoryTypeResponseDTO.builder().id(2).name("Mistura").build())));
        when(historyService.readAll()).thenReturn(new ArrayList<>(List.of(
                HistoryResponseDTO.builder().id(1).userId(1).historyTypeId(1).shelfId(1).executionDate(LocalDateTime.of(2026, 10, 5, 9, 0)).build(),
                HistoryResponseDTO.builder().id(2).userId(2).historyTypeId(2).shelfId(2).executionDate(LocalDateTime.of(2026, 10, 20, 9, 0)).build())));
        when(adminLogService.readAllNewestFirst()).thenReturn(new ArrayList<>(List.of(
                AdminLogResponseDTO.builder().id(1).adminId(1).affectedTable("produtos").recordId(1).action(AdminAction.INSERT)
                        .editDate(LocalDateTime.of(2026, 10, 5, 9, 0)).build(),
                AdminLogResponseDTO.builder().id(2).adminId(1).affectedTable("estantes").recordId(1).action(AdminAction.DELETE)
                        .editDate(LocalDateTime.of(2026, 10, 6, 9, 0)).build())));
    }

    private String page(String url) throws Exception {
        return mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    /** Conta os popups de edição (um por registro da página). */
    private static int rows(String html, String prefix) {
        Matcher matcher = Pattern.compile(Pattern.quote(prefix) + "\\d+\"").matcher(html);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    @Test
    void usersAreShownTenPerPageWithPaginationLinks() throws Exception {
        String first = page("/users");
        assertEquals(10, rows(first, "id=\"editar-user-"));
        assertTrue(first.contains("href=\"/users?page=2\"") && first.contains("Próxima página") && !first.contains("Página anterior"));

        String last = page("/users?page=3");
        assertEquals(6, rows(last, "id=\"editar-user-"));
        assertTrue(last.contains("Página anterior") && !last.contains("Próxima página"));

        assertEquals(6, rows(page("/users?page=99"), "id=\"editar-user-"), "página inexistente vira a última");
        assertEquals(10, rows(page("/users?page=abc"), "id=\"editar-user-"), "valor inválido vira a primeira");
    }

    @Test
    void usersCanBeFilteredByStatusTextAndRegistrationDate() throws Exception {
        String blocked = page("/users?status=BLOQUEADO");
        assertEquals(5, rows(blocked, "id=\"editar-user-"));
        assertFalse(blocked.contains("class=\"pagination\""), "uma página só não mostra a navegação");

        assertEquals(1, rows(page("/users?busca=user 07"), "id=\"editar-user-"));
        assertEquals(1, rows(page("/users?busca=JOAO"), "id=\"editar-user-"), "ignora acentos e maiúsculas");
        assertEquals(3, rows(page("/users?cadastroDe=2026-10-20&cadastroAte=2026-10-22"), "id=\"editar-user-"));
        assertEquals(10, rows(page("/users?cadastroDe=lixo"), "id=\"editar-user-"), "data inválida é ignorada");
    }

    @Test
    void paginationKeepsTheFiltersAndTheBarKeepsWhatWasTyped() throws Exception {
        assertTrue(page("/users?status=ATIVO").contains("page=2&amp;status=ATIVO"), "links da paginação mantêm os filtros");
        assertTrue(page("/users?busca=ana").contains("value=\"ana\""), "o campo volta preenchido");
        assertTrue(page("/users?status=BLOQUEADO").contains("<option value=\"BLOQUEADO\" selected"), "o select mantém a escolha");

        String reflected = mvc.perform(get("/users").param("busca", "<script>alert(1)</script>")).andReturn().getResponse().getContentAsString();
        assertTrue(reflected.contains("value=\"&lt;script&gt;alert(1)&lt;/script&gt;\"") && !reflected.contains("<script>alert"),
                "o valor digitado é escapado");
    }

    @Test
    void otherListsFilterByTheirOwnFields() throws Exception {
        assertEquals(1, rows(page("/companies?porte=GRANDE"), "id=\"editar-company-"));
        assertEquals(1, rows(page("/companies?busca=12.345.678"), "id=\"editar-company-"), "CNPJ com pontuação");
        assertEquals(1, rows(page("/products?empresa=2"), "id=\"editar-product-"));
        assertEquals(1, rows(page("/products?marca=lb"), "id=\"editar-product-"));
        assertEquals(1, rows(page("/products?busca=7647"), "id=\"editar-product-"));
        assertEquals(1, rows(page("/disposals?empresa=1"), "id=\"editar-disposal-"));
        assertEquals(1, rows(page("/disposals?metodo=VAZIO"), "id=\"editar-disposal-"));
        assertEquals(1, rows(page("/disposals?busca=cloro"), "id=\"editar-disposal-"));

        String shelves = page("/shelves?produto=1");
        assertTrue(shelves.contains("Lavanderia") && !shelves.contains("Cozinha</td>"));
        String withoutCoordinates = page("/locations?coordenadas=SEM");
        assertTrue(withoutCoordinates.contains("20000000") && !withoutCoordinates.contains("01310100"));
        String byZipCode = page("/locations?cep=01310-100");
        assertTrue(byZipCode.contains("01310100") && !byZipCode.contains("20000000"));
        String recent = page("/histories?execucaoDe=2026-10-10");
        assertTrue(recent.contains("<td>2</td>") && !recent.contains("<td>1</td>"));
        String ofType = page("/histories?tipo=1");
        assertTrue(ofType.contains("<td>1</td>") && !ofType.contains("<td>2</td>"));
        String deletions = page("/admin-logs?acao=DELETE");
        assertTrue(deletions.contains("estantes</td>") && !deletions.contains("produtos</td>"));
        String ofTable = page("/admin-logs?tabela=PRODUTOS");
        assertTrue(ofTable.contains("produtos</td>") && !ofTable.contains("estantes</td>"));
    }

    @Test
    void listsWithASinglePageShowNoPaginationAtAll() throws Exception {
        for (String path : new String[]{"/admins", "/companies", "/products", "/disposals", "/shelves",
                "/histories", "/history-types", "/locations", "/admin-logs"}) {
            assertFalse(page(path).contains("class=\"pagination\"") || page(path).contains("class = \"pagination\""), path);
        }
    }

    @Test
    void everyListOpensWithoutFilters() throws Exception {
        for (String path : new String[]{"/users", "/admins", "/companies", "/products", "/disposals", "/shelves",
                "/histories", "/history-types", "/locations", "/admin-logs"}) {
            page(path);
        }
    }
}
