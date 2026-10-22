package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.dto.DashboardDTO;
import com.quimia.quimiawebcrud.dto.HistoryProductResponseDTO;
import com.quimia.quimiawebcrud.dto.HistoryResponseDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.dto.ShelfProductResponseDTO;
import com.quimia.quimiawebcrud.dto.UserProductResponseDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private final UserService users = mock(UserService.class);
    private final CompanyService companies = mock(CompanyService.class);
    private final AdminService admins = mock(AdminService.class);
    private final ProductService products = mock(ProductService.class);
    private final HistoryService histories = mock(HistoryService.class);
    private final HistoryTypeService types = mock(HistoryTypeService.class);
    private final AdminLogService adminLogs = mock(AdminLogService.class);
    private final UserProductService userProducts = mock(UserProductService.class);
    private final ShelfProductService shelfProducts = mock(ShelfProductService.class);
    private final HistoryProductService historyProducts = mock(HistoryProductService.class);

    private DashboardService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
        service = new DashboardService(users, companies, admins, products, histories, types, adminLogs,
                userProducts, shelfProducts, historyProducts, clock);
        when(users.readAll()).thenReturn(List.of());
        when(companies.readAll()).thenReturn(List.of());
        when(admins.readAll()).thenReturn(List.of());
        when(products.readAll()).thenReturn(List.of());
        when(histories.readAll()).thenReturn(List.of());
        when(types.readAll()).thenReturn(List.of());
        when(adminLogs.readAll()).thenReturn(List.of());
        when(userProducts.readAll()).thenReturn(List.of());
        when(shelfProducts.readAll()).thenReturn(List.of());
        when(historyProducts.readAll()).thenReturn(List.of());
    }

    private static UserResponseDTO user(int id, String name, Status status, int day) {
        return UserResponseDTO.builder().id(id).userName(name).status(status)
                .signUpDate(LocalDateTime.of(2026, 10, day, 8, 0)).build();
    }

    private static HistoryResponseDTO history(int id, int userId, int typeId, LocalDateTime date) {
        return HistoryResponseDTO.builder().id(id).userId(userId).historyTypeId(typeId).executionDate(date).build();
    }

    @Test
    void emptySystemShowsZerosAndEmptyLists() {
        DashboardDTO dashboard = service.build(7);

        assertEquals(0, dashboard.getTotalUsers());
        assertEquals(0, dashboard.getTotalAlerts());
        assertEquals(7, dashboard.getActivity().getDots().size());
        assertEquals(4, dashboard.getActivity().getMax(), "sem registros a escala vai de 0 a 4");
        assertEquals(4, dashboard.getKpis().size());
        assertTrue(dashboard.getLatestActivities().isEmpty());
        assertTrue(dashboard.getRecentAlerts().isEmpty());
        assertTrue(dashboard.getTopProducts().isEmpty());
        assertEquals(0, dashboard.getQueriesByType().getTotal());
        assertTrue(dashboard.getQueriesByType().getSlices().isEmpty());
    }

    @Test
    void countsTotalsAndAlertsOnlyForPendingOrBlockedAccounts() {
        when(users.readAll()).thenReturn(List.of(
                user(1, "Ana", Status.ATIVO, 1), user(2, "Bia", Status.PENDENTE, 5), user(3, "Caio", Status.BLOQUEADO, 9)));
        when(companies.readAll()).thenReturn(List.of(
                CompanyResponseDTO.builder().id(1).name("Limpa Bem").status(Status.INATIVO).build(),
                CompanyResponseDTO.builder().id(2).name("Brilho").status(Status.PENDENTE)
                        .signUpDate(LocalDateTime.of(2026, 10, 7, 8, 0)).build()));
        when(admins.readAll()).thenReturn(List.of(AdminResponseDTO.builder().id(1).userName("Adm").status(Status.ATIVO).build()));
        when(products.readAll()).thenReturn(List.of(ProductResponseDTO.builder().id(1).name("Cloro").build()));

        DashboardDTO dashboard = service.build(7);

        assertEquals(3, dashboard.getTotalUsers());
        assertEquals(2, dashboard.getTotalCompanies());
        assertEquals(1, dashboard.getTotalProducts());
        assertEquals(3, dashboard.getTotalAlerts());
        List<String> names = dashboard.getRecentAlerts().stream().map(DashboardDTO.AlertItem::getName).toList();
        assertEquals(List.of("Caio", "Brilho", "Bia"), names, "newest first");
    }

    @Test
    void chartCountsOnlyTheSelectedPeriodAndComparesWithThePreviousOne() {
        when(histories.readAll()).thenReturn(List.of(
                history(1, 1, 1, TODAY.atTime(9, 0)),
                history(2, 1, 1, TODAY.atTime(15, 0)),
                history(3, 1, 1, TODAY.minusDays(6).atTime(10, 0)),
                history(4, 1, 1, TODAY.minusDays(7).atTime(10, 0)),
                history(5, 1, 1, TODAY.plusDays(1).atTime(10, 0))));

        DashboardDTO week = service.build(7);
        DashboardDTO.LineChart chart = week.getActivity();

        assertEquals(7, chart.getDots().size());
        assertEquals(3, week.getPeriodTotal(), "yesterday-7 and tomorrow are outside the window");
        assertEquals(3, chart.getCurrentTotal());
        assertEquals(1, chart.getPreviousTotal(), "the record 7 days ago belongs to the previous period");
        assertEquals("+200%", chart.getTrendText());
        assertEquals(DashboardDTO.Trend.UP, chart.getTrend());
        assertEquals(4, chart.getMax());
        assertEquals("44.0", chart.getDots().get(0).getX(), "first day at the left edge");
        assertEquals("628.0", chart.getDots().get(6).getX(), "today at the right edge");
        assertEquals("133.0", chart.getDots().get(6).getY(), "two records: half of the 0..4 scale");
        assertEquals("191.5", chart.getDots().get(0).getY());
        assertEquals("250.0", chart.getDots().get(3).getY(), "a day without records sits on the baseline");
        assertTrue(chart.getDots().get(6).getTitle().contains("2 registros"));
        assertTrue(chart.getCurrentLine().startsWith("M44.0,191.5 C"));
        assertTrue(chart.getCurrentArea().endsWith("Z"));

        assertEquals(4, service.build(30).getPeriodTotal());
        assertEquals(30, service.build(30).getActivity().getDots().size());
    }

    @Test
    void cardsShowTotalsAndHowSignupsChangedAgainstThePreviousPeriod() {
        when(users.readAll()).thenReturn(List.of(
                user(1, "Ana", Status.ATIVO, 20), user(2, "Bia", Status.ATIVO, 18),   // 2 nos últimos 7 dias
                user(3, "Caio", Status.ATIVO, 10),                                    // 1 no período anterior
                user(4, "Davi", Status.ATIVO, 1)));
        when(companies.readAll()).thenReturn(List.of(
                CompanyResponseDTO.builder().id(1).name("Limpa Bem").status(Status.ATIVO)
                        .signUpDate(LocalDateTime.of(2026, 10, 2, 8, 0)).build()));
        when(histories.readAll()).thenReturn(List.of(history(1, 1, 1, TODAY.atTime(9, 0)),
                history(2, 1, 1, TODAY.minusDays(8).atTime(9, 0)), history(3, 1, 1, TODAY.minusDays(9).atTime(9, 0))));

        List<DashboardDTO.Kpi> kpis = service.build(7).getKpis();

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
        assertEquals(null, kpis.get(3).getTrendText(), "alertas não têm variação");
    }

    @Test
    void signupsChartCountsTheLastSixMonthsOfUsersAndCompanies() {
        when(users.readAll()).thenReturn(List.of(user(1, "Ana", Status.ATIVO, 5), user(2, "Bia", Status.ATIVO, 6),
                UserResponseDTO.builder().id(3).userName("Caio").status(Status.ATIVO).signUpDate(LocalDateTime.of(2026, 7, 3, 8, 0)).build(),
                UserResponseDTO.builder().id(4).userName("Davi").status(Status.ATIVO).signUpDate(LocalDateTime.of(2025, 1, 3, 8, 0)).build()));
        when(companies.readAll()).thenReturn(List.of(CompanyResponseDTO.builder().id(1).name("Limpa Bem").status(Status.ATIVO)
                .signUpDate(LocalDateTime.of(2026, 10, 2, 8, 0)).build()));

        DashboardDTO.MonthlyChart chart = service.build(7).getSignups();

        assertEquals(6, chart.getMonths().size());
        assertEquals(List.of("Mai", "Jun", "Jul", "Ago", "Set", "Out"), chart.getMonths().stream().map(DashboardDTO.MonthGroup::getLabel).toList());
        DashboardDTO.MonthGroup october = chart.getMonths().get(5);
        assertEquals(2, october.getUsers().getCount());
        assertEquals(1, october.getCompanies().getCount());
        assertEquals(1, chart.getMonths().get(2).getUsers().getCount());
        assertEquals(4, chart.getMax());
        assertEquals(117, october.getUsers().getHeight(), "dois usuários: metade da escala 0..4");
        assertEquals(133, october.getUsers().getY());
    }

    @Test
    void invalidPeriodFallsBackToSevenDays() {
        assertEquals(7, DashboardService.normalizePeriod(5));
        assertEquals(7, DashboardService.normalizePeriod(-1));
        assertEquals(30, DashboardService.normalizePeriod(30));
        assertEquals(90, DashboardService.normalizePeriod(90));
        assertEquals(7, service.build(9999).getPeriodDays());
    }

    @Test
    void productRankingAndQueryDonutAreSortedAndScaled() {
        when(types.readAll()).thenReturn(List.of(
                HistoryTypeResponseDTO.builder().id(1).name("Descarte").build(),
                HistoryTypeResponseDTO.builder().id(2).name("Mistura").build()));
        when(histories.readAll()).thenReturn(List.of(
                history(1, 1, 1, TODAY.atTime(9, 0)), history(2, 1, 1, TODAY.atTime(9, 5)),
                history(3, 1, 1, TODAY.atTime(9, 6)), history(4, 1, 1, TODAY.atTime(9, 7)),
                history(5, 1, 2, TODAY.atTime(9, 8)), history(6, 1, 2, TODAY.atTime(9, 9))));
        when(products.readAll()).thenReturn(List.of(
                ProductResponseDTO.builder().id(1).name("Cloro").build(),
                ProductResponseDTO.builder().id(2).name("Detergente").build()));
        when(userProducts.readAll()).thenReturn(List.of(
                UserProductResponseDTO.builder().userId(1).productId(1).build(),
                UserProductResponseDTO.builder().userId(2).productId(1).build(),
                UserProductResponseDTO.builder().userId(1).productId(2).build()));
        when(shelfProducts.readAll()).thenReturn(List.of(ShelfProductResponseDTO.builder().shelfId(1).productId(1).build()));
        when(historyProducts.readAll()).thenReturn(List.of(HistoryProductResponseDTO.builder().historyId(1).productId(1).build()));

        DashboardDTO dashboard = service.build(7);

        assertEquals(6, dashboard.getQueriesByType().getTotal());
        assertEquals("Descarte", dashboard.getQueriesByType().getSlices().get(0).getName());
        assertEquals(4, dashboard.getQueriesByType().getSlices().get(0).getCount());
        assertEquals(67, dashboard.getQueriesByType().getSlices().get(0).getPercent());
        assertEquals(33, dashboard.getQueriesByType().getSlices().get(1).getPercent());
        assertEquals("Cloro", dashboard.getTopProducts().get(0).getName());
        assertEquals(4, dashboard.getTopProducts().get(0).getCount(), "user lists + shelf + history");
        assertEquals(25, dashboard.getTopProducts().get(1).getPercent());
    }

    @Test
    void latestActivitiesMixUsageAndAdminChangesNewestFirstAndLimitedToSix() {
        when(users.readAll()).thenReturn(List.of(user(1, "Ana", Status.ATIVO, 1)));
        when(admins.readAll()).thenReturn(List.of(AdminResponseDTO.builder().id(1).userName("Adm").status(Status.ATIVO).build()));
        when(types.readAll()).thenReturn(List.of(HistoryTypeResponseDTO.builder().id(1).name("Descarte").build()));
        java.util.ArrayList<HistoryResponseDTO> many = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            many.add(history(i, 1, 1, TODAY.atTime(8, i)));
        }
        when(histories.readAll()).thenReturn(many);
        when(adminLogs.readAll()).thenReturn(List.of(AdminLogResponseDTO.builder().id(1).adminId(1).affectedTable("produtos")
                .action(AdminAction.DELETE).editDate(TODAY.atTime(23, 0)).build()));

        List<DashboardDTO.ActivityItem> items = service.build(7).getLatestActivities();

        assertEquals(6, items.size());
        assertEquals("Adm excluiu um registro em produtos", items.get(0).getText());
        assertEquals("Ana registrou: Descarte", items.get(1).getText());
        assertTrue(items.get(1).getDate().isAfter(items.get(5).getDate()) || items.get(1).getDate().isEqual(items.get(5).getDate()));
    }

    @Test
    void aRestoreLineIsDescribedAsRestoredNotCreated() {
        when(admins.readAll()).thenReturn(List.of(AdminResponseDTO.builder().id(1).userName("Adm").status(Status.ATIVO).build()));
        when(adminLogs.readAll()).thenReturn(List.of(AdminLogResponseDTO.builder().id(1).adminId(1).affectedTable("usuarios")
                .action(AdminAction.INSERT).restore(true).editDate(TODAY.atTime(23, 0)).build()));

        assertEquals("Adm restaurou um registro em usuarios", service.build(7).getLatestActivities().get(0).getText());
    }

    @Test
    void usesTheInjectedClockForToday() {
        Instant other = LocalDate.of(2027, 1, 15).atTime(9, 0).atZone(ZONE).toInstant();
        DashboardService later = new DashboardService(users, companies, admins, products, histories, types, adminLogs,
                userProducts, shelfProducts, historyProducts, Clock.fixed(other, ZONE));
        when(histories.readAll()).thenReturn(List.of(history(1, 1, 1, LocalDateTime.of(2027, 1, 15, 8, 0))));

        assertEquals(1, later.build(7).getPeriodTotal());
        assertEquals(0, service.build(7).getPeriodTotal());
    }
}
