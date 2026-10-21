package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.dto.DashboardDTO;
import com.quimia.quimiawebcrud.dto.DashboardDTO.ActivityItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.AlertItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.RankItem;
import com.quimia.quimiawebcrud.dto.HistoryResponseDTO;
import com.quimia.quimiawebcrud.dto.HistoryTypeResponseDTO;
import com.quimia.quimiawebcrud.dto.ProductResponseDTO;
import com.quimia.quimiawebcrud.dto.UserResponseDTO;
import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta os números da tela inicial a partir dos dados já cadastrados.
 * Os cálculos são feitos em memória, o que atende bem o volume de dados do projeto.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    public static final int DEFAULT_PERIOD = 7;
    private static final Set<Integer> PERIODS = Set.of(7, 30, 90);
    private static final Set<Status> ATTENTION_STATUS = Set.of(Status.PENDENTE, Status.BLOQUEADO);
    private static final int LIST_SIZE = 5;
    private static final int ACTIVITY_SIZE = 6;

    private final UserService userService;
    private final CompanyService companyService;
    private final AdminService adminService;
    private final ProductService productService;
    private final HistoryService historyService;
    private final HistoryTypeService historyTypeService;
    private final AdminLogService adminLogService;
    private final UserProductService userProductService;
    private final ShelfProductService shelfProductService;
    private final HistoryProductService historyProductService;
    private final Clock clock;

    /**
     * Períodos aceitos no seletor do gráfico; qualquer outro valor vira o padrão (7 dias).
     */
    public static int normalizePeriod(int requested) {
        return PERIODS.contains(requested) ? requested : DEFAULT_PERIOD;
    }

    public DashboardDTO build(int requestedPeriod) {
        int period = normalizePeriod(requestedPeriod);
        List<UserResponseDTO> users = userService.readAll();
        List<CompanyResponseDTO> companies = companyService.readAll();
        List<AdminResponseDTO> admins = adminService.readAll();
        List<ProductResponseDTO> products = productService.readAll();
        List<HistoryResponseDTO> histories = historyService.readAll();
        List<HistoryTypeResponseDTO> types = historyTypeService.readAll();

        List<AlertItem> alerts = alerts(users, companies, admins);
        ActivityChart chart = chart(histories, period);
        long periodTotal = chart.bars().stream().mapToLong(bar -> bar.getCount()).sum();

        return new DashboardDTO(users.size(), companies.size(), products.size(), alerts.size(), period, periodTotal,
                chart.bars(), chart.linePoints(), chart.max(),
                latestActivities(histories, users, types, admins),
                alerts.stream().limit(LIST_SIZE).toList(),
                topProducts(products),
                topQueries(histories, types));
    }

    private ActivityChart chart(List<HistoryResponseDTO> histories, int period) {
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDay = today.minusDays(period - 1L);
        Map<LocalDate, Long> countsByDay = histories.stream()
                .map(HistoryResponseDTO::getExecutionDate)
                .filter(date -> date != null)
                .map(LocalDateTime::toLocalDate)
                .filter(day -> !day.isBefore(firstDay) && !day.isAfter(today))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return new ActivityChart(countsByDay, today, period);
    }

    /**
     * Contas que pedem atenção: usuários, empresas e administradores pendentes ou bloqueados, as mais novas primeiro.
     */
    private List<AlertItem> alerts(List<UserResponseDTO> users, List<CompanyResponseDTO> companies,
                                   List<AdminResponseDTO> admins) {
        List<AlertItem> alerts = new ArrayList<>();
        for (UserResponseDTO user : users) {
            if (ATTENTION_STATUS.contains(user.getStatus())) {
                alerts.add(new AlertItem("Usuário", user.getUserName(), user.getStatus(), user.getSignUpDate(), "/users"));
            }
        }
        for (CompanyResponseDTO company : companies) {
            if (ATTENTION_STATUS.contains(company.getStatus())) {
                alerts.add(new AlertItem("Empresa", company.getName(), company.getStatus(), company.getSignUpDate(), "/companies"));
            }
        }
        for (AdminResponseDTO admin : admins) {
            if (ATTENTION_STATUS.contains(admin.getStatus())) {
                alerts.add(new AlertItem("Administrador", admin.getUserName(), admin.getStatus(), admin.getSignUpDate(), "/admins"));
            }
        }
        alerts.sort(Comparator.comparing(AlertItem::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return alerts;
    }

    private List<ActivityItem> latestActivities(List<HistoryResponseDTO> histories, List<UserResponseDTO> users,
                                                List<HistoryTypeResponseDTO> types, List<AdminResponseDTO> admins) {
        Map<Integer, String> userNames = users.stream().collect(Collectors.toMap(UserResponseDTO::getId, UserResponseDTO::getUserName, (a, b) -> a));
        Map<Integer, String> typeNames = types.stream().collect(Collectors.toMap(HistoryTypeResponseDTO::getId, HistoryTypeResponseDTO::getName, (a, b) -> a));
        Map<Integer, String> adminNames = admins.stream().collect(Collectors.toMap(AdminResponseDTO::getId, AdminResponseDTO::getUserName, (a, b) -> a));

        List<ActivityItem> items = new ArrayList<>();
        for (HistoryResponseDTO history : histories) {
            String user = userNames.getOrDefault(history.getUserId(), "Usuário");
            String type = typeNames.getOrDefault(history.getHistoryTypeId(), "uma consulta");
            items.add(new ActivityItem(user + " registrou: " + type, history.getExecutionDate()));
        }
        for (AdminLogResponseDTO log : adminLogService.readAll()) {
            String admin = adminNames.getOrDefault(log.getAdminId(), "Administrador");
            items.add(new ActivityItem(admin + " " + verb(log) + " um registro em " + log.getAffectedTable(), log.getEditDate()));
        }
        items.sort(Comparator.comparing(ActivityItem::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return items.stream().limit(ACTIVITY_SIZE).toList();
    }

    private String verb(AdminLogResponseDTO log) {
        if (log.getAction() == null) {
            return "alterou";
        }
        return switch (log.getAction()) {
            case INSERT -> "criou";
            case DELETE -> "excluiu";
            default -> "alterou";
        };
    }

    /**
     * Produtos mais usados: quantas vezes aparecem nas listas de usuários, nas estantes e nos históricos.
     */
    private List<RankItem> topProducts(List<ProductResponseDTO> products) {
        Map<Integer, Long> uses = new HashMap<>();
        userProductService.readAll().forEach(link -> uses.merge(link.getProductId(), 1L, Long::sum));
        shelfProductService.readAll().forEach(link -> uses.merge(link.getProductId(), 1L, Long::sum));
        historyProductService.readAll().forEach(link -> uses.merge(link.getProductId(), 1L, Long::sum));
        Map<Integer, String> names = products.stream().collect(Collectors.toMap(ProductResponseDTO::getId, ProductResponseDTO::getName, (a, b) -> a));
        Map<String, Long> byName = new LinkedHashMap<>();
        uses.forEach((id, count) -> byName.merge(names.getOrDefault(id, "Produto #" + id), count, Long::sum));
        return ranking(byName);
    }

    /**
     * Consultas mais frequentes: quantos históricos existem de cada tipo.
     */
    private List<RankItem> topQueries(List<HistoryResponseDTO> histories, List<HistoryTypeResponseDTO> types) {
        Map<Integer, String> names = types.stream().collect(Collectors.toMap(HistoryTypeResponseDTO::getId, HistoryTypeResponseDTO::getName, (a, b) -> a));
        Map<String, Long> byName = new LinkedHashMap<>();
        histories.forEach(history -> byName.merge(names.getOrDefault(history.getHistoryTypeId(), "Sem tipo"), 1L, Long::sum));
        return ranking(byName);
    }

    private List<RankItem> ranking(Map<String, Long> counts) {
        List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(LIST_SIZE)
                .toList();
        long highest = sorted.isEmpty() ? 1 : Math.max(1, sorted.get(0).getValue());
        return sorted.stream()
                .map(entry -> new RankItem(entry.getKey(), entry.getValue(), (int) Math.round(entry.getValue() * 100.0 / highest)))
                .toList();
    }
}
