package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.DashboardDTO;
import com.quimia.quimiawebcrud.dto.DashboardDTO.ActivityItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.AlertItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.Kpi;
import com.quimia.quimiawebcrud.dto.DashboardDTO.LineChart;
import com.quimia.quimiawebcrud.dto.DashboardDTO.RankItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.Trend;
import com.quimia.quimiawebcrud.exception.DashboardUnavailableException;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.DashboardRepository;
import com.quimia.quimiawebcrud.repository.DashboardRepository.Row;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monta os números da tela inicial a partir da view materializada mv_dashboard, numa única leitura.
 * A view já traz os totais, os alertas, as últimas atividades, as séries por dia e por mês e os rankings; aqui só
 * se separa cada seção, se comparam os períodos e se calculam os gráficos em SVG. Os números valem até a próxima
 * atualização da view (ver {@code updatedAt}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    public static final int DEFAULT_PERIOD = 7;
    private static final Set<Integer> PERIODS = Set.of(7, 30, 90);
    private static final int LIST_SIZE = 5;
    private static final int ACTIVITY_SIZE = 6;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** "Ana Usuário (BLOQUEADO)", "Limpa Bem Empresa (PENDENTE)": o texto de cada alerta da view. */
    private static final Pattern ALERT_TEXT = Pattern.compile("^(.*) (Usuário|Empresa|Administrador) \\((\\w+)\\)$");

    private final DashboardRepository repository;
    private final Clock clock;
    private final ReentrantLock refreshLock = new ReentrantLock();

    /**
     * Períodos aceitos no seletor dos gráficos; qualquer outro valor vira o padrão (7 dias).
     */
    public static int normalizePeriod(int requested) {
        return PERIODS.contains(requested) ? requested : DEFAULT_PERIOD;
    }

    public DashboardDTO build(int requestedPeriod) {
        int period = normalizePeriod(requestedPeriod);
        LocalDate today = LocalDate.now(clock);
        List<Row> rows = read();

        long users = 0;
        long companies = 0;
        long products = 0;
        long alerts = 0;
        LocalDateTime updatedAt = null;
        List<AlertItem> alertItems = new ArrayList<>();
        List<ActivityItem> activities = new ArrayList<>();
        Map<LocalDate, Long> usageByDay = new HashMap<>();
        Map<LocalDate, Long> userSignupsByDay = new HashMap<>();
        Map<LocalDate, Long> companySignupsByDay = new HashMap<>();
        Map<YearMonth, Long> userSignupsByMonth = new HashMap<>();
        Map<YearMonth, Long> companySignupsByMonth = new HashMap<>();
        Map<String, Long> productUses = new LinkedHashMap<>();
        Map<String, Long> queryCounts = new LinkedHashMap<>();

        for (Row row : rows) {
            if (updatedAt == null) {
                updatedAt = row.updatedAt();
            }
            switch (row.section() == null ? "" : row.section()) {
                case DashboardRepository.TOTALS -> {
                    users = orZero(row.users());
                    companies = orZero(row.companies());
                    products = orZero(row.products());
                    alerts = orZero(row.alerts());
                }
                case DashboardRepository.ALERT -> alertItems.add(alertOf(row.alert()));
                case DashboardRepository.ACTIVITY -> activities.add(new ActivityItem(
                        row.admin() + " " + verb(row.action()) + " um registro em " + row.affectedTable(), row.editDate()));
                case DashboardRepository.USAGE_ACTIVITY -> activities.add(new ActivityItem(
                        row.admin() + " registrou: " + row.queryType(), row.editDate()));
                case DashboardRepository.USAGE_DAY -> addDay(usageByDay, row);
                case DashboardRepository.USER_SIGNUP_DAY -> addDay(userSignupsByDay, row);
                case DashboardRepository.COMPANY_SIGNUP_DAY -> addDay(companySignupsByDay, row);
                case DashboardRepository.USER_SIGNUP_MONTH -> addMonth(userSignupsByMonth, row);
                case DashboardRepository.COMPANY_SIGNUP_MONTH -> addMonth(companySignupsByMonth, row);
                case DashboardRepository.PRODUCT ->
                        productUses.merge(row.productName(), orZero(row.productUses()), Long::sum);
                case DashboardRepository.QUERY_TYPE ->
                        queryCounts.merge(row.queryType(), orZero(row.queryCount()), Long::sum);
                default -> log.warn("Seção desconhecida na mv_dashboard: {}", row.section());
            }
        }

        LineChart activity = ActivityChart.build(usageByDay, usageByDay, today, period);
        List<Kpi> kpis = List.of(
                signupsKpi("Usuários", users, userSignupsByDay, today, period, "/users", true),
                signupsKpi("Empresas", companies, companySignupsByDay, today, period, "/companies", false),
                new Kpi("Consultas", activity.getCurrentTotal(), "registros de uso nos últimos " + period + " dias",
                        activity.getTrendText(), activity.getTrend(), false, "/histories"),
                new Kpi("Alertas", alerts, "contas pendentes ou bloqueadas", null, Trend.FLAT, false, null));

        activities.sort(Comparator.comparing(ActivityItem::getDate, Comparator.nullsLast(Comparator.reverseOrder())));

        return new DashboardDTO(todayLabel(today), updatedAt, period, users, companies, products, alerts,
                activity.getCurrentTotal(), kpis, activity,
                DonutChart.build(queryCounts),
                SignupsChart.build(userSignupsByMonth, companySignupsByMonth, YearMonth.from(today)),
                activities.stream().limit(ACTIVITY_SIZE).toList(),
                alertItems.stream().limit(LIST_SIZE).toList(),
                ranking(productUses));
    }

    /**
     * Atualiza a view com os dados de agora. Só uma atualização roda por vez: um segundo clique enquanto a primeira
     * ainda está em andamento é recusado em vez de enfileirar outro cálculo pesado no banco.
     */
    public void refresh() {
        if (!refreshLock.tryLock()) {
            throw new DashboardUnavailableException(
                    "Já existe uma atualização do dashboard em andamento. Aguarde alguns instantes.");
        }
        try {
            repository.refresh();
        } catch (DataAccessException e) {
            log.error("Não foi possível atualizar a mv_dashboard", e);
            throw new DashboardUnavailableException(
                    "Não foi possível atualizar o dashboard agora. Tente novamente em instantes.");
        } finally {
            refreshLock.unlock();
        }
    }

    /** Falha ao ler a view (ela não existe, não foi populada, banco fora do ar...) vira um aviso para a tela. */
    private List<Row> read() {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            log.error("Não foi possível ler a mv_dashboard", e);
            throw new DashboardUnavailableException(
                    "Não foi possível carregar o dashboard agora. Tente novamente em instantes.");
        }
    }

    private static void addDay(Map<LocalDate, Long> series, Row row) {
        if (row.day() != null) {
            series.merge(row.day(), orZero(row.dayTotal()), Long::sum);
        }
    }

    /** Na view, o mês vem como o primeiro dia dele. */
    private static void addMonth(Map<YearMonth, Long> series, Row row) {
        if (row.day() != null) {
            series.merge(YearMonth.from(row.day()), orZero(row.dayTotal()), Long::sum);
        }
    }

    private static long orZero(Long value) {
        return value == null ? 0 : value;
    }

    /** Cartão de total com a variação dos cadastros do período contra o período anterior. */
    private static Kpi signupsKpi(String label, long total, Map<LocalDate, Long> signupsByDay, LocalDate today, int period,
                                  String path, boolean highlight) {
        long current = countBetween(signupsByDay, today.minusDays(period - 1L), today);
        long previous = countBetween(signupsByDay, today.minusDays(2L * period - 1), today.minusDays(period));
        return new Kpi(label, total, current + (current == 1 ? " novo" : " novos") + " nos últimos " + period + " dias",
                ChartScale.trendText(current, previous), ChartScale.trend(current, previous), highlight, path);
    }

    private static long countBetween(Map<LocalDate, Long> countsByDay, LocalDate from, LocalDate to) {
        return countsByDay.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(from) && !entry.getKey().isAfter(to))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    private static String todayLabel(LocalDate today) {
        String text = today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PT_BR));
        return text.substring(0, 1).toUpperCase(PT_BR) + text.substring(1);
    }

    /**
     * Separa o tipo, o nome e o status do texto do alerta para a tela mostrar o selo e o link da lista de origem.
     */
    private static AlertItem alertOf(String text) {
        Matcher matcher = text == null ? null : ALERT_TEXT.matcher(text);
        if (matcher != null && matcher.matches()) {
            try {
                Status status = Status.valueOf(matcher.group(3));
                return new AlertItem(matcher.group(2), matcher.group(1), status, pathOf(matcher.group(2)));
            } catch (IllegalArgumentException e) {
                // status que o sistema não conhece: cai no texto inteiro, abaixo
            }
        }
        return new AlertItem(null, text == null ? "" : text, null, null);
    }

    private static String pathOf(String kind) {
        return switch (kind) {
            case "Usuário" -> "/users";
            case "Empresa" -> "/companies";
            default -> "/admins";
        };
    }

    private static String verb(String action) {
        if (action == null) {
            return "alterou";
        }
        return switch (action) {
            case "INSERT" -> "criou";
            case "DELETE" -> "excluiu";
            default -> "alterou";
        };
    }

    private static List<RankItem> ranking(Map<String, Long> counts) {
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
