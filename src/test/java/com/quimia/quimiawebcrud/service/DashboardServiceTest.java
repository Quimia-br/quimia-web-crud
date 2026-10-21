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
        assertEquals(7, dashboard.getBars().size());
        assertEquals(1, dashboard.getChartMax());
        assertTrue(dashboard.getLatestActivities().isEmpty());
        assertTrue(dashboard.getRecentAlerts().isEmpty());
        assertTrue(dashboard.getTopProducts().isEmpty());
        assertTrue(dashboard.getTopQueries().isEmpty());
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
    void chartCountsOnlyTheSelectedPeriodAndKeepsEmptyDays() {
        when(histories.readAll()).thenReturn(List.of(
                history(1, 1, 1, TODAY.atTime(9, 0)),
                history(2, 1, 1, TODAY.atTime(15, 0)),
                history(3, 1, 1, TODAY.minusDays(6).atTime(10, 0)),
                history(4, 1, 1, TODAY.minusDays(7).atTime(10, 0)),
                history(5, 1, 1, TODAY.plusDays(1).atTime(10, 0))));

        DashboardDTO week = service.build(7);

        assertEquals(7, week.getBars().size());
        assertEquals(3, week.getPeriodTotal(), "yesterday-7 and tomorrow are outside the window");
        assertEquals(2, week.getBars().get(6).getCount(), "last bar is today");
        assertEquals(1, week.getBars().get(0).getCount(), "first bar is six days ago");
        assertEquals(0, week.getBars().get(3).getCount());
        assertEquals(2, week.getChartMax());
        assertEquals(180, week.getBars().get(6).getHeight(), "the tallest bar fills the plot");
        assertEquals(90, week.getBars().get(0).getHeight());

        assertEquals(4, service.build(30).getPeriodTotal());
        assertEquals(30, service.build(30).getBars().size());
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
    void rankingsAreSortedAndScaledToTheLeader() {
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

        assertEquals("Descarte", dashboard.getTopQueries().get(0).getName());
        assertEquals(4, dashboard.getTopQueries().get(0).getCount());
        assertEquals(100, dashboard.getTopQueries().get(0).getPercent());
        assertEquals(50, dashboard.getTopQueries().get(1).getPercent());
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
    void usesTheInjectedClockForToday() {
        Instant other = LocalDate.of(2027, 1, 15).atTime(9, 0).atZone(ZONE).toInstant();
        DashboardService later = new DashboardService(users, companies, admins, products, histories, types, adminLogs,
                userProducts, shelfProducts, historyProducts, Clock.fixed(other, ZONE));
        when(histories.readAll()).thenReturn(List.of(history(1, 1, 1, LocalDateTime.of(2027, 1, 15, 8, 0))));

        assertEquals(1, later.build(7).getPeriodTotal());
        assertEquals(0, service.build(7).getPeriodTotal());
    }
}
