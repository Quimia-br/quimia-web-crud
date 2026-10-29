package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.DashboardDTO;
import com.quimia.quimiawebcrud.exception.DashboardUnavailableException;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.DashboardRepository;
import com.quimia.quimiawebcrud.repository.DashboardRepository.Row;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O dashboard lê só a view materializada mv_dashboard: o service separa as seções das linhas e prepara os textos.
 */
class DashboardServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 10, 20, 12, 0);

    private final DashboardRepository repository = mock(DashboardRepository.class);
    private final Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
    private final DashboardService service = new DashboardService(repository, clock);

    private static Row totals(long users, long products, long companies, long alerts) {
        return new Row("totais", users, products, companies, alerts, null, null, null, null, null, null, null, null, null, null, null, UPDATED);
    }

    private static Row alert(String text) {
        return new Row("alerta", null, null, null, null, null, null, null, null, text, null, null, null, null, null, null, UPDATED);
    }

    private static Row activity(String admin, String action, String table, LocalDateTime date) {
        return new Row("atividade", null, null, null, null, admin, action, date, table, null, null, null, null, null, null, null, UPDATED);
    }

    private static Row day(LocalDate day, long total) {
        return new Row("atividade_dia", null, null, null, null, null, null, null, null, null, null, null, null, null, day, total, UPDATED);
    }

    private static Row product(String name, long uses) {
        return new Row("produto", null, null, null, null, null, null, null, null, null, uses, name, null, null, null, null, UPDATED);
    }

    private static Row queryType(String name, long count) {
        return new Row("tipo_consulta", null, null, null, null, null, null, null, null, null, null, null, count, name, null, null, UPDATED);
    }

    @Test
    void emptyViewShowsZerosAndEmptyLists() {
        when(repository.findAll()).thenReturn(List.of());

        DashboardDTO dashboard = service.build(7);

        assertEquals(0, dashboard.getTotalUsers());
        assertEquals(0, dashboard.getTotalAlerts());
        assertNull(dashboard.getUpdatedAt());
        assertEquals(7, dashboard.getBars().size());
        assertEquals(0, dashboard.getPeriodTotal());
        assertEquals(1, dashboard.getChartMax());
        assertTrue(dashboard.getLatestActivities().isEmpty());
        assertTrue(dashboard.getRecentAlerts().isEmpty());
        assertTrue(dashboard.getTopProducts().isEmpty());
        assertTrue(dashboard.getTopQueries().isEmpty());
    }

    @Test
    void totalsComeFromTheTotalsRowAndTheUpdateTimeFromTheView() {
        when(repository.findAll()).thenReturn(List.of(totals(123, 500, 2, 90)));

        DashboardDTO dashboard = service.build(7);

        assertEquals(123, dashboard.getTotalUsers());
        assertEquals(500, dashboard.getTotalProducts());
        assertEquals(2, dashboard.getTotalCompanies());
        assertEquals(90, dashboard.getTotalAlerts());
        assertEquals(UPDATED, dashboard.getUpdatedAt());
    }

    @Test
    void chartCountsOnlyTheSelectedPeriodAndKeepsEmptyDays() {
        when(repository.findAll()).thenReturn(List.of(
                day(TODAY, 2),
                day(TODAY.minusDays(6), 1),
                day(TODAY.minusDays(7), 5),
                day(TODAY.plusDays(1), 3)));

        DashboardDTO week = service.build(7);

        assertEquals(7, week.getBars().size());
        assertEquals(3, week.getPeriodTotal(), "o dia 7 atrás e o de amanhã ficam fora da janela");
        assertEquals(2, week.getBars().get(6).getCount(), "a última coluna é hoje");
        assertEquals(1, week.getBars().get(0).getCount(), "a primeira coluna é seis dias atrás");
        assertEquals(0, week.getBars().get(3).getCount());
        assertEquals(2, week.getChartMax());
        assertEquals(180, week.getBars().get(6).getHeight(), "a coluna mais alta preenche o gráfico");
        assertEquals(90, week.getBars().get(0).getHeight());

        DashboardDTO month = service.build(30);
        assertEquals(30, month.getBars().size());
        assertEquals(8, month.getPeriodTotal(), "no período de 30 dias entra o dia 7 atrás");
    }

    @Test
    void invalidPeriodFallsBackToSevenDays() {
        when(repository.findAll()).thenReturn(List.of());

        assertEquals(7, DashboardService.normalizePeriod(5));
        assertEquals(7, DashboardService.normalizePeriod(-1));
        assertEquals(30, DashboardService.normalizePeriod(30));
        assertEquals(90, DashboardService.normalizePeriod(90));
        assertEquals(7, service.build(9999).getPeriodDays());
    }

    @Test
    void usesTheInjectedClockForToday() {
        Instant other = LocalDate.of(2027, 1, 15).atTime(9, 0).atZone(ZONE).toInstant();
        DashboardService later = new DashboardService(repository, Clock.fixed(other, ZONE));
        when(repository.findAll()).thenReturn(List.of(day(LocalDate.of(2027, 1, 15), 4)));

        assertEquals(4, later.build(7).getPeriodTotal());
        assertEquals(0, service.build(7).getPeriodTotal());
    }

    @Test
    void alertTextIsSplitIntoKindNameAndStatusWithALinkToTheSourceList() {
        when(repository.findAll()).thenReturn(List.of(
                alert("Ana Souza Usuário (BLOQUEADO)"), alert("Limpa Bem (Filial) Empresa (PENDENTE)")));

        List<DashboardDTO.AlertItem> alerts = service.build(7).getRecentAlerts();

        assertEquals("Ana Souza", alerts.get(0).getName());
        assertEquals("Usuário", alerts.get(0).getKind());
        assertEquals(Status.BLOQUEADO, alerts.get(0).getStatus());
        assertEquals("/users", alerts.get(0).getPath());
        assertEquals("Limpa Bem (Filial)", alerts.get(1).getName(), "parênteses no nome não atrapalham");
        assertEquals(Status.PENDENTE, alerts.get(1).getStatus());
        assertEquals("/companies", alerts.get(1).getPath());
    }

    @Test
    void anAlertInAnUnexpectedFormatIsShownAsPlainText() {
        when(repository.findAll()).thenReturn(List.of(alert("algo diferente"), alert("Ana Usuário (OUTRO)")));

        List<DashboardDTO.AlertItem> alerts = service.build(7).getRecentAlerts();

        assertEquals("algo diferente", alerts.get(0).getName());
        assertNull(alerts.get(0).getStatus());
        assertNull(alerts.get(0).getPath());
        assertEquals("Ana Usuário (OUTRO)", alerts.get(1).getName(), "status desconhecido não vira link");
    }

    @Test
    void onlyTheFirstFiveAlertsAreShown() {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            rows.add(alert("Pessoa " + i + " Usuário (PENDENTE)"));
        }
        when(repository.findAll()).thenReturn(rows);

        assertEquals(5, service.build(7).getRecentAlerts().size());
    }

    @Test
    void activitiesDescribeWhatTheAdminDidAndAreLimitedToSix() {
        List<Row> rows = new ArrayList<>();
        rows.add(activity("Adm", "DELETE", "produtos", UPDATED));
        rows.add(activity("Adm", "INSERT", "usuarios", UPDATED.minusHours(1)));
        rows.add(activity("Bia", "UPDATE", "empresas", UPDATED.minusHours(2)));
        for (int i = 3; i < 10; i++) {
            rows.add(activity("Adm", "UPDATE", "produtos", UPDATED.minusHours(i)));
        }
        when(repository.findAll()).thenReturn(rows);

        List<DashboardDTO.ActivityItem> items = service.build(7).getLatestActivities();

        assertEquals(6, items.size());
        assertEquals("Adm excluiu um registro em produtos", items.get(0).getText());
        assertEquals("Adm criou um registro em usuarios", items.get(1).getText());
        assertEquals("Bia alterou um registro em empresas", items.get(2).getText());
        assertEquals(UPDATED, items.get(0).getDate());
    }

    @Test
    void rankingsAreSortedAndScaledToTheLeader() {
        when(repository.findAll()).thenReturn(List.of(
                product("Detergente", 1), product("Cloro", 4), product("Soda", 2),
                queryType("Mistura", 2), queryType("Descarte", 4)));

        DashboardDTO dashboard = service.build(7);

        assertEquals("Descarte", dashboard.getTopQueries().get(0).getName());
        assertEquals(100, dashboard.getTopQueries().get(0).getPercent());
        assertEquals(50, dashboard.getTopQueries().get(1).getPercent());
        assertEquals(List.of("Cloro", "Soda", "Detergente"), dashboard.getTopProducts().stream().map(DashboardDTO.RankItem::getName).toList());
        assertEquals(4, dashboard.getTopProducts().get(0).getCount());
        assertEquals(25, dashboard.getTopProducts().get(2).getPercent());
    }

    @Test
    void anUnknownSectionIsIgnored() {
        when(repository.findAll()).thenReturn(List.of(
                new Row("nova", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, UPDATED),
                totals(1, 2, 3, 4)));

        assertEquals(1, service.build(7).getTotalUsers());
    }

    @Test
    void aFailureReadingTheViewBecomesADashboardUnavailableMessage() {
        when(repository.findAll()).thenThrow(new BadSqlGrammarException("lê a view", "select ... from mv_dashboard",
                new java.sql.SQLException("relation \"mv_dashboard\" does not exist")));

        DashboardUnavailableException error = assertThrows(DashboardUnavailableException.class, () -> service.build(7));

        assertEquals("Não foi possível carregar o dashboard agora. Tente novamente em instantes.", error.getMessage());
        assertTrue(!error.getMessage().contains("mv_dashboard"), "o detalhe do banco não chega à tela");

        doThrow(new DataAccessResourceFailureException("sem conexão")).when(repository).findAll();
        assertThrows(DashboardUnavailableException.class, () -> service.build(7));
    }
}
