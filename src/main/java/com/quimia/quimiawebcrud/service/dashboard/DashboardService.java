package com.quimia.quimiawebcrud.service.dashboard;

import com.quimia.quimiawebcrud.dto.response.DashboardDTO.ActivityItem;
import com.quimia.quimiawebcrud.dto.response.DashboardDTO.AlertItem;
import com.quimia.quimiawebcrud.dto.response.DashboardDTO.Kpi;
import com.quimia.quimiawebcrud.dto.response.DashboardDTO.LineChart;
import com.quimia.quimiawebcrud.dto.response.DashboardDTO.RankItem;
import com.quimia.quimiawebcrud.dto.response.DashboardDTO.Trend;
import com.quimia.quimiawebcrud.dto.response.DashboardDTO;
import com.quimia.quimiawebcrud.exception.DashboardUnavailableException;
import com.quimia.quimiawebcrud.model.enums.Status;
import com.quimia.quimiawebcrud.repository.support.DashboardRepository.Row;
import com.quimia.quimiawebcrud.repository.support.DashboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
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

    /** "Ana Usuário (BLOQUEADO)", "Limpa Bem Empresa (PENDENTE)": o texto de cada alerta da view. */
    private static final Pattern ALERT_TEXT = Pattern.compile("^(.*) (Usuário|Empresa|Administrador) \\((\\w+)\\)$");

    private final DashboardRepository repository;
    private final Clock clock;
    private final MessageSource messages;
    private final ReentrantLock refreshLock = new ReentrantLock();

    /**
     * Períodos aceitos no seletor dos gráficos; qualquer outro valor vira o padrão (7 dias).
     */
    public static int normalizePeriod(int requested) {
        return PERIODS.contains(requested) ? requested : DEFAULT_PERIOD;
    }

    /** Lê a view e monta a tela inicial para o período pedido (7, 30 ou 90 dias), com os textos no idioma pedido. */
    public DashboardDTO build(int requestedPeriod, Locale locale) {
        int period = normalizePeriod(requestedPeriod);
        DashboardTexts texts = new DashboardTexts(messages, locale);
        LocalDate today = LocalDate.now(clock);
        Sections view = Sections.from(read(texts), texts);

        LineChart activity = ActivityChart.build(view.usageByDay, view.usageByDay, today, period, texts);
        List<Kpi> kpis = List.of(
                signupsKpi(texts.get("dash.kpi.users"), view.users, view.userSignupsByDay, today, period, "/users", true, texts),
                signupsKpi(texts.get("dash.kpi.companies"), view.companies, view.companySignupsByDay, today, period, "/companies", false, texts),
                new Kpi(texts.get("dash.kpi.queries"), activity.getCurrentTotal(), texts.get("dash.kpi.queriesCaption", period),
                        activity.getTrendText(), activity.getTrend(), false, "/histories"),
                new Kpi(texts.get("dash.kpi.alerts"), view.alerts, texts.get("dash.kpi.alertsCaption"), null, Trend.FLAT, false, null));

        return new DashboardDTO(todayLabel(today, texts), view.updatedAt, period, view.users, view.companies, view.products, view.alerts,
                activity.getCurrentTotal(), kpis, activity,
                DonutChart.build(view.queryCounts, texts),
                SignupsChart.build(view.userSignupsByMonth, view.companySignupsByMonth, YearMonth.from(today), texts),
                view.recentActivities(),
                view.alertItems.stream().limit(LIST_SIZE).toList(),
                ranking(view.productUses));
    }

    /**
     * Atualiza a view com os dados de agora. Só uma atualização roda por vez: um segundo clique enquanto a primeira
     * ainda está em andamento é recusado em vez de enfileirar outro cálculo pesado no banco.
     */
    public void refresh(Locale locale) {
        if (!refreshLock.tryLock()) {
            throw new DashboardUnavailableException(messages.getMessage("dash.error.refreshing", null, locale));
        }
        try {
            repository.refresh();
        } catch (DataAccessException e) {
            log.error("Não foi possível atualizar a mv_dashboard", e);
            throw new DashboardUnavailableException(messages.getMessage("dash.error.refreshFailed", null, locale));
        } finally {
            refreshLock.unlock();
        }
    }

    /** Falha ao ler a view (ela não existe, não foi populada, banco fora do ar...) vira um aviso para a tela. */
    private List<Row> read(DashboardTexts texts) {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            log.error("Não foi possível ler a mv_dashboard", e);
            throw new DashboardUnavailableException(texts.get("dash.error.loadFailed"));
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
                                  String path, boolean highlight, DashboardTexts texts) {
        long current = countBetween(signupsByDay, today.minusDays(period - 1L), today);
        long previous = countBetween(signupsByDay, today.minusDays(2L * period - 1), today.minusDays(period));
        return new Kpi(label, total, texts.get(current == 1 ? "dash.kpi.newOne" : "dash.kpi.newMany", current, period),
                ChartScale.trendText(current, previous, texts.get("dash.trend.new")), ChartScale.trend(current, previous),
                highlight, path);
    }

    private static long countBetween(Map<LocalDate, Long> countsByDay, LocalDate from, LocalDate to) {
        return countsByDay.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(from) && !entry.getKey().isAfter(to))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    private static String todayLabel(LocalDate today, DashboardTexts texts) {
        String text = today.format(DateTimeFormatter.ofPattern(texts.get("dash.date.pattern"), texts.locale()));
        return text.substring(0, 1).toUpperCase(texts.locale()) + text.substring(1);
    }

    /**
     * Separa o tipo, o nome e o status do texto do alerta para a tela mostrar o selo e o link da lista de origem.
     */
    private static AlertItem alertOf(String text, DashboardTexts texts) {
        Matcher matcher = text == null ? null : ALERT_TEXT.matcher(text);
        if (matcher != null && matcher.matches()) {
            try {
                Status status = Status.valueOf(matcher.group(3));
                return new AlertItem(kindLabel(matcher.group(2), texts), matcher.group(1), status, pathOf(matcher.group(2)));
            } catch (IllegalArgumentException e) {
                // status que o sistema não conhece: cai no texto inteiro, abaixo
            }
        }
        return new AlertItem(null, text == null ? "" : text, null, null);
    }

    /** O tipo que a view escreve em português (Usuário, Empresa, Administrador), no idioma do pedido. */
    private static String kindLabel(String kind, DashboardTexts texts) {
        return switch (kind) {
            case "Usuário" -> texts.get("dash.kind.user");
            case "Empresa" -> texts.get("dash.kind.company");
            default -> texts.get("dash.kind.admin");
        };
    }

    private static String pathOf(String kind) {
        return switch (kind) {
            case "Usuário" -> "/users";
            case "Empresa" -> "/companies";
            default -> "/admins";
        };
    }

    /** A frase de uma edição do log: quem criou, alterou ou excluiu um registro e em qual tabela. */
    private static String editSentence(String admin, String action, String table, DashboardTexts texts) {
        String key = switch (action == null ? "" : action) {
            case "INSERT" -> "dash.activity.created";
            case "DELETE" -> "dash.activity.deleted";
            default -> "dash.activity.updated";
        };
        return texts.get(key, admin, table);
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

    /**
     * Cada seção da view separada em sua própria coleção. A view devolve tudo numa tabela só, com uma coluna
     * dizendo de que lista a linha faz parte; o {@code switch} em {@link #add} faz essa separação.
     */
    private static final class Sections {
        long users;
        long companies;
        long products;
        long alerts;
        LocalDateTime updatedAt;
        final List<AlertItem> alertItems = new ArrayList<>();
        final List<ActivityItem> activities = new ArrayList<>();
        final Map<LocalDate, Long> usageByDay = new HashMap<>();
        final Map<LocalDate, Long> userSignupsByDay = new HashMap<>();
        final Map<LocalDate, Long> companySignupsByDay = new HashMap<>();
        final Map<YearMonth, Long> userSignupsByMonth = new HashMap<>();
        final Map<YearMonth, Long> companySignupsByMonth = new HashMap<>();
        final Map<String, Long> productUses = new LinkedHashMap<>();
        final Map<String, Long> queryCounts = new LinkedHashMap<>();

        private final DashboardTexts texts;

        private Sections(DashboardTexts texts) {
            this.texts = texts;
        }

        static Sections from(List<Row> rows, DashboardTexts texts) {
            Sections sections = new Sections(texts);
            rows.forEach(sections::add);
            return sections;
        }

        /** As atividades mais recentes primeiro, só as que cabem na tela. */
        List<ActivityItem> recentActivities() {
            return activities.stream()
                    .sorted(Comparator.comparing(ActivityItem::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(ACTIVITY_SIZE)
                    .toList();
        }

        private void add(Row row) {
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
                case DashboardRepository.ALERT -> alertItems.add(alertOf(row.alert(), texts));
                case DashboardRepository.ACTIVITY -> activities.add(new ActivityItem(
                        editSentence(row.admin(), row.action(), row.affectedTable(), texts), row.editDate()));
                case DashboardRepository.USAGE_ACTIVITY -> activities.add(new ActivityItem(
                        texts.get("dash.activity.usage", row.admin(), row.queryType()), row.editDate()));
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
    }
}
