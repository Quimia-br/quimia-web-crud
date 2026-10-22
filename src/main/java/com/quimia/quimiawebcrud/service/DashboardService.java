package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.dto.AdminResponseDTO;
import com.quimia.quimiawebcrud.dto.CompanyResponseDTO;
import com.quimia.quimiawebcrud.dto.DashboardDTO;
import com.quimia.quimiawebcrud.dto.DashboardDTO.ActivityItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.AlertItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.Kpi;
import com.quimia.quimiawebcrud.dto.DashboardDTO.LineChart;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

        LocalDate today = LocalDate.now(clock);
        List<AlertItem> alerts = alerts(users, companies, admins);
        LineChart activity = activity(histories, today, period);
        List<LocalDateTime> userDates = users.stream().map(UserResponseDTO::getSignUpDate).toList();
        List<LocalDateTime> companyDates = companies.stream().map(CompanyResponseDTO::getSignUpDate).toList();

        List<Kpi> kpis = List.of(
                signupsKpi("Usuários", users.size(), userDates, today, period, "/users", true),
                signupsKpi("Empresas", companies.size(), companyDates, today, period, "/companies", false),
                new Kpi("Consultas", activity.getCurrentTotal(), "registros de uso nos últimos " + period + " dias",
                        activity.getTrendText(), activity.getTrend(), false, "/histories"),
                new Kpi("Alertas", alerts.size(), "contas pendentes ou bloqueadas", null, DashboardDTO.Trend.FLAT,
                        false, null));

        return new DashboardDTO(todayLabel(today), period, users.size(), companies.size(), products.size(),
                alerts.size(), activity.getCurrentTotal(), kpis, activity,
                DonutChart.build(queriesByType(histories, types)),
                SignupsChart.build(userDates, companyDates, YearMonth.from(today)),
                latestActivities(histories, users, types, admins),
                alerts.stream().limit(LIST_SIZE).toList(),
                topProducts(products));
    }

    private LineChart activity(List<HistoryResponseDTO> histories, LocalDate today, int period) {
        LocalDate firstDay = today.minusDays(2L * period - 1);
        Map<LocalDate, Long> countsByDay = histories.stream()
                .map(HistoryResponseDTO::getExecutionDate)
                .filter(date -> date != null)
                .map(LocalDateTime::toLocalDate)
                .filter(day -> !day.isBefore(firstDay) && !day.isAfter(today))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return ActivityChart.build(countsByDay, countsByDay, today, period);
    }

    /** Cartão de total com a variação dos cadastros do período contra o período anterior. */
    private Kpi signupsKpi(String label, long total, List<LocalDateTime> dates, LocalDate today, int period,
                           String path, boolean highlight) {
        long current = countBetween(dates, today.minusDays(period - 1L), today);
        long previous = countBetween(dates, today.minusDays(2L * period - 1), today.minusDays(period));
        return new Kpi(label, total, current + (current == 1 ? " novo" : " novos") + " nos últimos " + period + " dias",
                ChartScale.trendText(current, previous), ChartScale.trend(current, previous), highlight, path);
    }

    private long countBetween(List<LocalDateTime> dates, LocalDate from, LocalDate to) {
        return dates.stream().filter(date -> date != null).map(LocalDateTime::toLocalDate)
                .filter(day -> !day.isBefore(from) && !day.isAfter(to)).count();
    }

    private String todayLabel(LocalDate today) {
        String text = today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR")));
        return text.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-BR")) + text.substring(1);
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
        if (log.isRestore()) {
            return "restaurou";
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
     * Consultas por tipo: quantos históricos existem de cada tipo (a rosca mostra as maiores e agrupa o resto).
     */
    private Map<String, Long> queriesByType(List<HistoryResponseDTO> histories, List<HistoryTypeResponseDTO> types) {
        Map<Integer, String> names = types.stream().collect(Collectors.toMap(HistoryTypeResponseDTO::getId, HistoryTypeResponseDTO::getName, (a, b) -> a));
        Map<String, Long> byName = new LinkedHashMap<>();
        histories.forEach(history -> byName.merge(names.getOrDefault(history.getHistoryTypeId(), "Sem tipo"), 1L, Long::sum));
        return byName;
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
