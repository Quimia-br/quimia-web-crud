package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.dto.DashboardDTO;
import com.quimia.quimiawebcrud.dto.DashboardDTO.ActivityItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.AlertItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.ChartBar;
import com.quimia.quimiawebcrud.dto.DashboardDTO.RankItem;
import com.quimia.quimiawebcrud.exception.DashboardUnavailableException;
import com.quimia.quimiawebcrud.exception.GlobalExceptionHandler;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A tela inicial mostra o que a view materializada trouxe e, se a view não puder ser lida, um aviso com status 503.
 */
@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardPageTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DashboardService dashboardService;

    private static DashboardDTO dashboard(List<AlertItem> alerts) {
        return new DashboardDTO(10, 2, 30, 4, LocalDateTime.of(2026, 10, 20, 14, 5),
                7, 3, List.of(new ChartBar("Seg", 3, 50, 20, 40, 180, true, 70, "20/10/2026: 3")), "70,20", 3,
                List.of(new ActivityItem("Adm excluiu um registro em produtos", LocalDateTime.of(2026, 10, 20, 9, 30))),
                alerts,
                List.of(new RankItem("Cloro", 4, 100)),
                List.of(new RankItem("Descarte", 3, 75)));
    }

    private String page() throws Exception {
        return mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void showsTotalsRankingsActivityAndWhenTheViewWasUpdated() throws Exception {
        when(dashboardService.build(7)).thenReturn(dashboard(List.of()));

        String html = page();

        assertTrue(html.contains("Dados atualizados em"));
        assertTrue(html.contains("20/10/2026 14:05"));
        assertTrue(html.contains("Adm excluiu um registro em produtos"));
        assertTrue(html.contains("Cloro") && html.contains("Descarte"));
        assertTrue(html.contains("Nenhuma conta pendente ou bloqueada."));
    }

    @Test
    void showsTheAdminActivityChartWithThePeriodSelector() throws Exception {
        when(dashboardService.build(7)).thenReturn(dashboard(List.of()));
        when(dashboardService.build(30)).thenReturn(dashboard(List.of()));

        String html = page();

        assertTrue(html.contains("Atividade dos administradores ao longo do tempo"));
        assertTrue(html.contains("<svg class=\"chart\""));
        assertTrue(html.contains("<title>20/10/2026: 3</title>"));
        assertTrue(html.contains("points=\"70,20\""));
        assertTrue(html.contains("href=\"/?periodo=30\""));
        assertTrue(html.contains("3</strong> alterações de administradores nos últimos"));
        mvc.perform(get("/").param("periodo", "30")).andExpect(status().isOk());
        verify(dashboardService).build(30);
    }

    @Test
    void alertsLinkToTheirListWithABadgeAndOddOnesAreJustText() throws Exception {
        when(dashboardService.build(7)).thenReturn(dashboard(List.of(
                new AlertItem("Usuário", "Ana", Status.BLOQUEADO, "/users"),
                new AlertItem(null, "algo diferente", null, null))));

        String html = page();

        assertTrue(html.contains("href=\"/users?busca=Ana&amp;status=BLOQUEADO\""));
        assertTrue(html.contains("algo diferente"));
    }

    @Test
    void whenTheViewCannotBeReadTheScreenSaysSoWith503() throws Exception {
        when(dashboardService.build(7)).thenThrow(new DashboardUnavailableException(
                "Não foi possível carregar o dashboard agora. Tente novamente em instantes."));

        String html = mvc.perform(get("/")).andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("Não foi possível carregar o dashboard agora."));
    }
}
