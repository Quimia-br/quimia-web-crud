package com.quimia.quimiawebcrud.controller;

import com.quimia.quimiawebcrud.exception.DashboardUnavailableException;
import com.quimia.quimiawebcrud.exception.GlobalExceptionHandler;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.repository.DashboardRepository;
import com.quimia.quimiawebcrud.repository.DashboardRepository.Row;
import com.quimia.quimiawebcrud.service.CurrentAdmin;
import com.quimia.quimiawebcrud.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A tela inicial renderizada a partir das linhas da view: saudação, cartões com variação, gráficos em SVG, nenhuma
 * regra que a política de segurança (CSP) bloquearia, como estilos ou scripts dentro da página, e o aviso (503)
 * quando a view não pode ser lida.
 */
@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardPageTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 10, 22, 14, 5);

    @Autowired
    MockMvc mvc;
    @MockitoBean
    DashboardService dashboardService;
    @MockitoBean
    CurrentAdmin currentAdmin;

    private static Row row(String section, Long users, Long products, Long companies, Long alerts, String admin,
                           String action, LocalDateTime editDate, String table, String alert, Long uses, String productName,
                           Long queryCount, String queryType, LocalDate day, Long dayTotal) {
        return new Row(section, users, products, companies, alerts, admin, action, editDate, table, alert, uses,
                productName, queryCount, queryType, day, dayTotal, UPDATED);
    }

    private static Row series(String section, LocalDate day, long total) {
        return row(section, null, null, null, null, null, null, null, null, null, null, null, null, null, day, total);
    }

    private static Row alert(String text) {
        return row("alerta", null, null, null, null, null, null, null, null, text, null, null, null, null, null, null);
    }

    private DashboardService serviceWithSomeData(List<Row> extra) {
        List<Row> rows = new ArrayList<>(List.of(
                row("totais", 12L, 1L, 1L, 3L, null, null, null, null, null, null, null, null, null, null, null),
                row("atividade", null, null, null, null, "Adm", "DELETE", TODAY.atTime(9, 0), "produtos", null, null, null, null, null, null, null),
                row("atividade_uso", null, null, null, null, "Ana", null, TODAY.atTime(10, 0), null, null, null, null, null, "Descarte", null, null),
                row("produto", null, null, null, null, null, null, null, null, null, 4L, "Cloro", null, null, null, null),
                row("tipo_consulta", null, null, null, null, null, null, null, null, null, null, null, 4L, "Descarte", null, null),
                row("tipo_consulta", null, null, null, null, null, null, null, null, null, null, null, 3L, "Mistura", null, null),
                series("cadastro_usuario_mes", LocalDate.of(2026, 10, 1), 5),
                series("cadastro_empresa_mes", LocalDate.of(2026, 10, 1), 1),
                series("cadastro_usuario_dia", TODAY.minusDays(2), 2),
                series("uso_dia", TODAY, 3),
                series("uso_dia", TODAY.minusDays(1), 2)));
        rows.addAll(extra);
        DashboardRepository repository = mock(DashboardRepository.class);
        when(repository.findAll()).thenReturn(rows);
        return new DashboardService(repository, Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE));
    }

    private String page(String period, List<Row> extra) throws Exception {
        DashboardService real = serviceWithSomeData(extra);
        when(dashboardService.build(anyInt())).thenAnswer(invocation -> real.build(invocation.getArgument(0)));
        when(currentAdmin.require()).thenReturn(Admin.builder().id(1).userName("Nicolas Delecrode").email("n@x.com").build());
        return mvc.perform(get("/").param("periodo", period)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void showsTheGreetingTheCardsAndTheChartsForEachPeriod() throws Exception {
        for (String period : new String[]{"7", "30", "90"}) {
            String html = page(period, List.of());

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
            assertTrue(html.contains("Adm excluiu um registro em produtos") && html.contains("Ana registrou: Descarte"));
        }
    }

    @Test
    void showsWhenTheViewWasLastRefreshed() throws Exception {
        String html = page("7", List.of());

        assertTrue(html.contains("dados atualizados em"));
        assertTrue(html.contains("22/10/2026 14:05"));
    }

    @Test
    void pageHasNothingTheContentSecurityPolicyWouldBlock() throws Exception {
        String html = page("7", List.of());

        assertFalse(html.contains(" style="), "estilos ficam no arquivo CSS, não na página");
        assertFalse(html.contains("<script"), "o dashboard não usa JavaScript");
        assertFalse(html.contains("onclick=") || html.contains("onload="));
    }

    @Test
    void periodSelectorMarksTheChosenPeriod() throws Exception {
        String html = page("30", List.of());

        assertTrue(html.contains("periodo=30\" aria-current=\"true\""), "o período escolhido fica marcado");
        assertTrue(html.contains("nos últimos 30 dias"));
    }

    @Test
    void alertsLinkToTheirListWithABadgeAndOddOnesAreJustText() throws Exception {
        String html = page("7", List.of(alert("Ana Usuário (BLOQUEADO)"), alert("Adm Administrador (PENDENTE)"),
                alert("algo diferente")));

        assertTrue(html.contains("href=\"/users?busca=Ana&amp;status=BLOQUEADO\""));
        assertTrue(html.contains("href=\"/admins?busca=Adm&amp;status=PENDENTE\""));
        assertTrue(html.contains("algo diferente"));
    }

    @Test
    void theRefreshButtonPostsToTheRefreshRouteKeepingThePeriod() throws Exception {
        String html = page("30", List.of());

        assertTrue(html.contains("Atualizar dashboard"));
        assertTrue(html.contains("action=\"/dashboard/refresh\""));
        assertTrue(html.contains("name=\"periodo\" value=\"30\""), "volta para o mesmo período");
    }

    @Test
    void refreshingRecalculatesTheViewAndComesBackToTheSamePeriod() throws Exception {
        mvc.perform(post("/dashboard/refresh").param("periodo", "30"))
                .andExpect(redirectedUrl("/?periodo=30"))
                .andExpect(flash().attribute("message", "Dashboard atualizado."));
        verify(dashboardService).refresh();

        mvc.perform(post("/dashboard/refresh").param("periodo", "5"))
                .andExpect(redirectedUrl("/?periodo=7"));
    }

    @Test
    void whenTheRefreshFailsTheReasonShowsAsANoticeInsteadOfAnErrorPage() throws Exception {
        doThrow(new DashboardUnavailableException("Não foi possível atualizar o dashboard agora. Tente novamente em instantes."))
                .when(dashboardService).refresh();

        mvc.perform(post("/dashboard/refresh"))
                .andExpect(redirectedUrl("/?periodo=7"))
                .andExpect(flash().attribute("error", "Não foi possível atualizar o dashboard agora. Tente novamente em instantes."));
    }

    @Test
    void whenTheViewCannotBeReadTheScreenSaysSoWith503() throws Exception {
        when(currentAdmin.require()).thenReturn(Admin.builder().id(1).userName("Nicolas").email("n@x.com").build());
        when(dashboardService.build(anyInt())).thenThrow(new DashboardUnavailableException(
                "Não foi possível carregar o dashboard agora. Tente novamente em instantes."));

        String html = mvc.perform(get("/")).andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("Não foi possível carregar o dashboard agora."));
    }
}
