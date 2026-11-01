package com.quimia.quimiawebcrud.service.dashboard;

import com.quimia.quimiawebcrud.TestMessages;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O dashboard lê só a view materializada mv_dashboard: o service separa as seções das linhas, compara os períodos
 * e prepara os gráficos.
 */
class DashboardServiceTest {

    private static final java.util.Locale PT = TestMessages.PT;

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 10, 20, 12, 0);

    private final DashboardRepository repository = mock(DashboardRepository.class);
    private final Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
    private final DashboardService service = new DashboardService(repository, clock, TestMessages.source());

    private static Row row(String section, Long users, Long products, Long companies, Long alerts, String admin,
                           String action, LocalDateTime editDate, String table, String alert, Long uses, String productName,
                           Long queryCount, String queryType, LocalDate day, Long dayTotal) {
        return new Row(section, users, products, companies, alerts, admin, action, editDate, table, alert, uses,
                productName, queryCount, queryType, day, dayTotal, UPDATED);
    }

    private static Row totals(long users, long products, long companies, long alerts) {
        return row("totais", users, products, companies, alerts, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static Row alert(String text) {
        return row("alerta", null, null, null, null, null, null, null, null, text, null, null, null, null, null, null);
    }

    private static Row adminActivity(String admin, String action, String table, LocalDateTime date) {
        return row("atividade", null, null, null, null, admin, action, date, table, null, null, null, null, null, null, null);
    }

    private static Row usageActivity(String user, String type, LocalDateTime date) {
        return row("atividade_uso", null, null, null, null, user, null, date, null, null, null, null, null, type, null, null);
    }

    private static Row series(String section, LocalDate day, long total) {
        return row(section, null, null, null, null, null, null, null, null, null, null, null, null, null, day, total);
    }

    private static Row product(String name, long uses) {
        return row("produto", null, null, null, null, null, null, null, null, null, uses, name, null, null, null, null);
    }

    private static Row queryType(String name, long count) {
        return row("tipo_consulta", null, null, null, null, null, null, null, null, null, null, null, count, name, null, null);
    }

    @Test
    void emptyViewShowsZerosAndEmptyLists() {
        when(repository.findAll()).thenReturn(List.of());

        DashboardDTO dashboard = service.build(7, PT);

        assertEquals(0, dashboard.getTotalUsers());
        assertEquals(0, dashboard.getTotalAlerts());
        assertNull(dashboard.getUpdatedAt());
        assertEquals(7, dashboard.getActivity().getDots().size());
        assertEquals(4, dashboard.getActivity().getMax(), "sem registros a escala vai de 0 a 4");
        assertEquals(4, dashboard.getKpis().size());
        assertEquals(6, dashboard.getSignups().getMonths().size());
        assertTrue(dashboard.getLatestActivities().isEmpty());
        assertTrue(dashboard.getRecentAlerts().isEmpty());
        assertTrue(dashboard.getTopProducts().isEmpty());
        assertEquals(0, dashboard.getQueriesByType().getTotal());
    }

    @Test
    void totalsComeFromTheTotalsRowAndTheUpdateTimeFromTheView() {
        when(repository.findAll()).thenReturn(List.of(totals(123, 500, 2, 90)));

        DashboardDTO dashboard = service.build(7, PT);

        assertEquals(123, dashboard.getTotalUsers());
        assertEquals(500, dashboard.getTotalProducts());
        assertEquals(2, dashboard.getTotalCompanies());
        assertEquals(90, dashboard.getTotalAlerts());
        assertEquals(UPDATED, dashboard.getUpdatedAt());
        assertEquals("Terça-feira, 20 de outubro de 2026", dashboard.getTodayLabel());
    }

    @Test
    void chartCountsOnlyTheSelectedPeriodAndComparesWithThePreviousOne() {
        when(repository.findAll()).thenReturn(List.of(
                series("uso_dia", TODAY, 2),
                series("uso_dia", TODAY.minusDays(6), 1),
                series("uso_dia", TODAY.minusDays(7), 1),
                series("uso_dia", TODAY.plusDays(1), 3)));

        DashboardDTO week = service.build(7, PT);
        DashboardDTO.LineChart chart = week.getActivity();

        assertEquals(7, chart.getDots().size());
        assertEquals(3, week.getPeriodTotal(), "o dia 7 atrás e o de amanhã ficam fora da janela");
        assertEquals(3, chart.getCurrentTotal());
        assertEquals(1, chart.getPreviousTotal(), "o registro de 7 dias atrás pertence ao período anterior");
        assertEquals("+200%", chart.getTrendText());
        assertEquals(DashboardDTO.Trend.UP, chart.getTrend());
        assertEquals(4, chart.getMax());
        assertEquals("44.0", chart.getDots().get(0).getX(), "o primeiro dia fica na borda esquerda");
        assertEquals("628.0", chart.getDots().get(6).getX(), "hoje fica na borda direita");
        assertEquals("133.0", chart.getDots().get(6).getY(), "dois registros: metade da escala 0..4");
        assertEquals("191.5", chart.getDots().get(0).getY());
        assertEquals("250.0", chart.getDots().get(3).getY(), "dia sem registro fica na linha de base");
        assertTrue(chart.getDots().get(6).getTitle().contains("2 registros"));
        assertTrue(chart.getCurrentLine().startsWith("M44.0,191.5 C"));
        assertTrue(chart.getCurrentArea().endsWith("Z"));

        assertEquals(4, service.build(30, PT).getPeriodTotal());
        assertEquals(30, service.build(30, PT).getActivity().getDots().size());
    }

    @Test
    void cardsShowTotalsAndHowSignupsChangedAgainstThePreviousPeriod() {
        when(repository.findAll()).thenReturn(List.of(
                totals(4, 0, 1, 0),
                series("cadastro_usuario_dia", LocalDate.of(2026, 10, 20), 1),   // 2 nos últimos 7 dias
                series("cadastro_usuario_dia", LocalDate.of(2026, 10, 18), 1),
                series("cadastro_usuario_dia", LocalDate.of(2026, 10, 10), 1),   // 1 no período anterior
                series("cadastro_usuario_dia", LocalDate.of(2026, 10, 1), 1),
                series("cadastro_empresa_dia", LocalDate.of(2026, 10, 2), 1),
                series("uso_dia", TODAY, 1),
                series("uso_dia", TODAY.minusDays(8), 1),
                series("uso_dia", TODAY.minusDays(9), 1)));

        List<DashboardDTO.Kpi> kpis = service.build(7, PT).getKpis();

        assertEquals(List.of("Usuários", "Empresas", "Consultas", "Alertas"), kpis.stream().map(DashboardDTO.Kpi::getLabel).toList());
        DashboardDTO.Kpi usersCard = kpis.get(0);
        assertEquals(4, usersCard.getValue());
        assertTrue(usersCard.isHighlight());
        assertEquals("2 novos nos últimos 7 dias", usersCard.getCaption());
        assertEquals("+100%", usersCard.getTrendText());
        assertEquals(DashboardDTO.Trend.UP, usersCard.getTrend());
        assertEquals(DashboardDTO.Trend.FLAT, kpis.get(1).getTrend(), "nenhuma empresa nova nos dois períodos");
        assertEquals("0%", kpis.get(1).getTrendText());
        DashboardDTO.Kpi queries = kpis.get(2);
        assertEquals(1, queries.getValue());
        assertEquals("-50%", queries.getTrendText());
        assertEquals(DashboardDTO.Trend.DOWN, queries.getTrend());
        assertNull(kpis.get(3).getTrendText(), "alertas não têm variação");
    }

    @Test
    void signupsChartCountsTheLastSixMonthsOfUsersAndCompanies() {
        when(repository.findAll()).thenReturn(List.of(
                series("cadastro_usuario_mes", LocalDate.of(2026, 10, 1), 2),
                series("cadastro_usuario_mes", LocalDate.of(2026, 7, 1), 1),
                series("cadastro_usuario_mes", LocalDate.of(2025, 1, 1), 9),
                series("cadastro_empresa_mes", LocalDate.of(2026, 10, 1), 1)));

        DashboardDTO.MonthlyChart chart = service.build(7, PT).getSignups();

        assertEquals(6, chart.getMonths().size());
        assertEquals(List.of("Mai", "Jun", "Jul", "Ago", "Set", "Out"), chart.getMonths().stream().map(DashboardDTO.MonthGroup::getLabel).toList());
        DashboardDTO.MonthGroup october = chart.getMonths().get(5);
        assertEquals(2, october.getUsers().getCount());
        assertEquals(1, october.getCompanies().getCount());
        assertEquals(1, chart.getMonths().get(2).getUsers().getCount());
        assertEquals(4, chart.getMax(), "o mês de 2025 fica fora da escala");
        assertEquals(117, october.getUsers().getHeight(), "dois usuários: metade da escala 0..4");
        assertEquals(133, october.getUsers().getY());
    }

    @Test
    void invalidPeriodFallsBackToSevenDays() {
        when(repository.findAll()).thenReturn(List.of());

        assertEquals(7, DashboardService.normalizePeriod(5));
        assertEquals(7, DashboardService.normalizePeriod(-1));
        assertEquals(30, DashboardService.normalizePeriod(30));
        assertEquals(90, DashboardService.normalizePeriod(90));
        assertEquals(7, service.build(9999, PT).getPeriodDays());
    }

    @Test
    void usesTheInjectedClockForToday() {
        Instant other = LocalDate.of(2027, 1, 15).atTime(9, 0).atZone(ZONE).toInstant();
        DashboardService later = new DashboardService(repository, Clock.fixed(other, ZONE), TestMessages.source());
        when(repository.findAll()).thenReturn(List.of(series("uso_dia", LocalDate.of(2027, 1, 15), 4)));

        assertEquals(4, later.build(7, PT).getPeriodTotal());
        assertEquals(0, service.build(7, PT).getPeriodTotal());
    }

    @Test
    void productRankingAndQueryDonutAreSortedAndScaled() {
        when(repository.findAll()).thenReturn(List.of(
                product("Detergente", 1), product("Cloro", 4), product("Soda", 2),
                queryType("Mistura", 2), queryType("Descarte", 4)));

        DashboardDTO dashboard = service.build(7, PT);

        assertEquals(6, dashboard.getQueriesByType().getTotal());
        assertEquals("Descarte", dashboard.getQueriesByType().getSlices().get(0).getName());
        assertEquals(4, dashboard.getQueriesByType().getSlices().get(0).getCount());
        assertEquals(67, dashboard.getQueriesByType().getSlices().get(0).getPercent());
        assertEquals(33, dashboard.getQueriesByType().getSlices().get(1).getPercent());
        assertEquals(List.of("Cloro", "Soda", "Detergente"), dashboard.getTopProducts().stream().map(DashboardDTO.RankItem::getName).toList());
        assertEquals(4, dashboard.getTopProducts().get(0).getCount());
        assertEquals(25, dashboard.getTopProducts().get(2).getPercent());
    }

    @Test
    void latestActivitiesMixUsageAndAdminChangesNewestFirstAndLimitedToSix() {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            rows.add(usageActivity("Ana", "Descarte", TODAY.atTime(8, i)));
        }
        rows.add(adminActivity("Adm", "DELETE", "produtos", TODAY.atTime(23, 0)));
        rows.add(adminActivity("Adm", "INSERT", "usuarios", TODAY.atTime(7, 0)));
        when(repository.findAll()).thenReturn(rows);

        List<DashboardDTO.ActivityItem> items = service.build(7, PT).getLatestActivities();

        assertEquals(6, items.size());
        assertEquals("Adm excluiu um registro em produtos", items.get(0).getText());
        assertEquals("Ana registrou: Descarte", items.get(1).getText());
        assertEquals(TODAY.atTime(8, 7), items.get(1).getDate(), "o uso mais recente vem logo depois");
    }

    @Test
    void anAdminActionIsDescribedByItsKind() {
        when(repository.findAll()).thenReturn(List.of(
                adminActivity("Adm", "INSERT", "usuarios", TODAY.atTime(10, 0)),
                adminActivity("Bia", "UPDATE", "empresas", TODAY.atTime(9, 0))));

        List<DashboardDTO.ActivityItem> items = service.build(7, PT).getLatestActivities();

        assertEquals("Adm criou um registro em usuarios", items.get(0).getText());
        assertEquals("Bia alterou um registro em empresas", items.get(1).getText());
    }

    @Test
    void alertTextIsSplitIntoKindNameAndStatusWithALinkToTheSourceList() {
        when(repository.findAll()).thenReturn(List.of(
                alert("Ana Souza Usuário (BLOQUEADO)"), alert("Limpa Bem (Filial) Empresa (PENDENTE)"),
                alert("Adm Administrador (PENDENTE)")));

        List<DashboardDTO.AlertItem> alerts = service.build(7, PT).getRecentAlerts();

        assertEquals("Ana Souza", alerts.get(0).getName());
        assertEquals("Usuário", alerts.get(0).getKind());
        assertEquals(Status.BLOQUEADO, alerts.get(0).getStatus());
        assertEquals("/users", alerts.get(0).getPath());
        assertEquals("Limpa Bem (Filial)", alerts.get(1).getName(), "parênteses no nome não atrapalham");
        assertEquals("/companies", alerts.get(1).getPath());
        assertEquals("Administrador", alerts.get(2).getKind());
        assertEquals("/admins", alerts.get(2).getPath());
    }

    @Test
    void anAlertInAnUnexpectedFormatIsShownAsPlainText() {
        when(repository.findAll()).thenReturn(List.of(alert("algo diferente"), alert("Ana Usuário (OUTRO)")));

        List<DashboardDTO.AlertItem> alerts = service.build(7, PT).getRecentAlerts();

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

        assertEquals(5, service.build(7, PT).getRecentAlerts().size());
    }

    @Test
    void anUnknownSectionIsIgnored() {
        when(repository.findAll()).thenReturn(List.of(
                row("nova", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                totals(1, 2, 3, 4)));

        assertEquals(1, service.build(7, PT).getTotalUsers());
    }

    @Test
    void refreshAsksTheDatabaseToRecalculateTheView() {
        service.refresh(PT);

        verify(repository).refresh();
    }

    @Test
    void aFailureRefreshingTheViewBecomesAFriendlyMessageWithoutTheDatabaseDetail() {
        doThrow(new BadSqlGrammarException("atualiza a view", "refresh materialized view concurrently mv_dashboard",
                new java.sql.SQLException("must be owner of materialized view mv_dashboard"))).when(repository).refresh();

        DashboardUnavailableException error = assertThrows(DashboardUnavailableException.class, () -> service.refresh(PT));

        assertEquals("Não foi possível atualizar o dashboard agora. Tente novamente em instantes.", error.getMessage());
        assertTrue(!error.getMessage().contains("owner"), "o detalhe do banco não chega à tela");
        // a trava é liberada: a próxima tentativa roda normalmente
        doNothing().when(repository).refresh();
        service.refresh(PT);
    }

    @Test
    void aSecondRefreshWhileTheFirstIsRunningIsRefused() throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> second = new java.util.concurrent.atomic.AtomicReference<>();
        doAnswer(invocation -> {
            Thread other = new Thread(() -> {
                try {
                    service.refresh(PT);
                } catch (Throwable e) {
                    second.set(e);
                }
            });
            other.start();
            other.join();
            return null;
        }).when(repository).refresh();

        service.refresh(PT);

        assertTrue(second.get() instanceof DashboardUnavailableException);
        assertEquals("Já existe uma atualização do dashboard em andamento. Aguarde alguns instantes.", second.get().getMessage());
    }

    @Test
    void aFailureReadingTheViewBecomesADashboardUnavailableMessage() {
        when(repository.findAll()).thenThrow(new BadSqlGrammarException("lê a view", "select ... from mv_dashboard",
                new java.sql.SQLException("relation \"mv_dashboard\" does not exist")));

        DashboardUnavailableException error = assertThrows(DashboardUnavailableException.class, () -> service.build(7, PT));

        assertEquals("Não foi possível carregar o dashboard agora. Tente novamente em instantes.", error.getMessage());
        assertTrue(!error.getMessage().contains("mv_dashboard"), "o detalhe do banco não chega à tela");

        doThrow(new DataAccessResourceFailureException("sem conexão")).when(repository).findAll();
        assertThrows(DashboardUnavailableException.class, () -> service.build(7, PT));
    }
}
