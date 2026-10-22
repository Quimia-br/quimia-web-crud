package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.dto.HistoryResponseDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.dto.UserProductResponseDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.service.AdminLogService;
import com.quimia.quimiawebcrud.service.AdminService;
import com.quimia.quimiawebcrud.service.CompanyService;
import com.quimia.quimiawebcrud.service.CurrentAdmin;
import com.quimia.quimiawebcrud.service.DashboardService;
import com.quimia.quimiawebcrud.service.HistoryProductService;
import com.quimia.quimiawebcrud.service.HistoryService;
import com.quimia.quimiawebcrud.service.HistoryTypeService;
import com.quimia.quimiawebcrud.service.ProductService;
import com.quimia.quimiawebcrud.service.ShelfProductService;
import com.quimia.quimiawebcrud.service.UserProductService;
import com.quimia.quimiawebcrud.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * A tela inicial renderizada: saudação, cartões com variação, gráficos em SVG e nenhuma regra que a política de
 * segurança (CSP) bloquearia, como estilos ou scripts dentro da página.
 */
@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardPageTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    @Autowired
    MockMvc mvc;
    @MockitoBean
    DashboardService dashboardService;
    @MockitoBean
    CurrentAdmin currentAdmin;

    private DashboardService serviceWithSomeData() {
        UserService users = mock(UserService.class);
        CompanyService companies = mock(CompanyService.class);
        AdminService admins = mock(AdminService.class);
        ProductService products = mock(ProductService.class);
        HistoryService histories = mock(HistoryService.class);
        HistoryTypeService types = mock(HistoryTypeService.class);
        AdminLogService logs = mock(AdminLogService.class);
        UserProductService userProducts = mock(UserProductService.class);
        ShelfProductService shelfProducts = mock(ShelfProductService.class);
        HistoryProductService historyProducts = mock(HistoryProductService.class);

        List<UserResponseDTO> userList = new ArrayList<>();
        List<HistoryResponseDTO> historyList = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            userList.add(UserResponseDTO.builder().id(i).userName("Usuário " + i).status(i % 5 == 0 ? Status.BLOQUEADO : Status.ATIVO)
                    .signUpDate(TODAY.minusDays(i * 9L).atTime(9, 0)).build());
            historyList.add(HistoryResponseDTO.builder().id(i).userId(i).historyTypeId(1 + i % 3)
                    .executionDate(TODAY.minusDays(i % 6).atTime(10, i)).build());
        }
        when(users.readAll()).thenReturn(userList);
        when(companies.readAll()).thenReturn(List.of(CompanyResponseDTO.builder().id(1).name("Limpa Bem").status(Status.ATIVO)
                .signUpDate(TODAY.minusDays(2).atTime(9, 0)).build()));
        when(admins.readAll()).thenReturn(List.of(AdminResponseDTO.builder().id(1).userName("Adm").status(Status.ATIVO).build()));
        when(products.readAll()).thenReturn(List.of(ProductResponseDTO.builder().id(1).name("Cloro").build()));
        when(histories.readAll()).thenReturn(historyList);
        when(types.readAll()).thenReturn(List.of(HistoryTypeResponseDTO.builder().id(1).name("Descarte").build(),
                HistoryTypeResponseDTO.builder().id(2).name("Mistura").build(),
                HistoryTypeResponseDTO.builder().id(3).name("Limpeza").build()));
        when(logs.readAll()).thenReturn(List.of());
        when(userProducts.readAll()).thenReturn(List.of(UserProductResponseDTO.builder().userId(1).productId(1).build()));
        when(shelfProducts.readAll()).thenReturn(List.of());
        when(historyProducts.readAll()).thenReturn(List.of());
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
        return new DashboardService(users, companies, admins, products, histories, types, logs, userProducts, shelfProducts,
                historyProducts, clock);
    }

    private String page(String period) throws Exception {
        DashboardService real = serviceWithSomeData();
        when(dashboardService.build(anyInt())).thenAnswer(invocation -> real.build(invocation.getArgument(0)));
        when(currentAdmin.require()).thenReturn(Admin.builder().id(1).userName("Nicolas Delecrode").email("n@x.com").build());
        return mvc.perform(get("/").param("periodo", period)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void showsTheGreetingTheCardsAndTheChartsForEachPeriod() throws Exception {
        for (String period : new String[]{"7", "30", "90"}) {
            String html = page(period);

            assertTrue(html.contains("Bem-vindo de volta, <span>Nicolas</span>"), "saudação com o primeiro nome");
            assertTrue(html.contains("Quinta-feira, 22 de outubro de 2026"));
            for (String label : new String[]{">Usuários<", ">Empresas<", ">Consultas<", ">Alertas<"}) {
                assertTrue(html.contains(label), label);
            }
            assertTrue(html.contains("trend-up") || html.contains("trend-down") || html.contains("trend-flat"));
            assertTrue(html.contains("chart-line-current") && html.contains("chart-line-previous") && html.contains("chart-area"));
            assertTrue(html.contains("donut-slice-1") && html.contains(">consultas</text>"));
            assertTrue(html.contains("bar-users") && html.contains("bar-companies"));
            assertTrue(html.contains("Produtos mais utilizados") && html.contains("Cloro"));
        }
    }

    @Test
    void pageHasNothingTheContentSecurityPolicyWouldBlock() throws Exception {
        String html = page("7");

        assertFalse(html.contains(" style="), "estilos ficam no arquivo CSS, não na página");
        assertFalse(html.contains("<script"), "o dashboard não usa JavaScript");
        assertFalse(html.contains("onclick=") || html.contains("onload="));
    }

    @Test
    void periodSelectorMarksTheChosenPeriod() throws Exception {
        String html = page("30");

        assertTrue(html.contains("periodo=30\" aria-current=\"true\""), "o período escolhido fica marcado");
        assertTrue(html.contains("nos últimos 30 dias"));
    }
}
