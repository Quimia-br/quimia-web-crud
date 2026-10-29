package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.DashboardDTO;
import com.quimia.quimiawebcrud.dto.DashboardDTO.ActivityItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.AlertItem;
import com.quimia.quimiawebcrud.dto.DashboardDTO.ChartBar;
import com.quimia.quimiawebcrud.dto.DashboardDTO.RankItem;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Monta os números da tela inicial a partir da view materializada mv_dashboard, numa única leitura.
 * A view já traz os totais, os alertas, as últimas ações dos administradores e os rankings; aqui só se separa
 * cada seção e se prepara o texto. Os números valem até a próxima atualização da view (ver {@code updatedAt}).
 * O gráfico usa as alterações de administradores por dia (seção atividade_dia, últimos 90 dias).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    public static final int DEFAULT_PERIOD = 7;
    private static final Set<Integer> PERIODS = Set.of(7, 30, 90);
    private static final int LIST_SIZE = 5;
    private static final int ACTIVITY_SIZE = 6;

    /** "Ana Usuário (BLOQUEADO)" e "Limpa Bem Empresa (PENDENTE)": o texto de cada alerta da view. */
    private static final Pattern ALERT_TEXT = Pattern.compile("^(.*) (Usuário|Empresa) \\((\\w+)\\)$");

    private final DashboardRepository repository;
    private final Clock clock;

    /**
     * Períodos aceitos no seletor do gráfico; qualquer outro valor vira o padrão (7 dias).
     */
    public static int normalizePeriod(int requested) {
        return PERIODS.contains(requested) ? requested : DEFAULT_PERIOD;
    }

    public DashboardDTO build(int requestedPeriod) {
        int period = normalizePeriod(requestedPeriod);
        List<Row> rows = read();

        long users = 0;
        long companies = 0;
        long products = 0;
        long alerts = 0;
        LocalDateTime updatedAt = null;
        List<AlertItem> alertItems = new ArrayList<>();
        List<ActivityItem> activities = new ArrayList<>();
        Map<LocalDate, Long> countsByDay = new HashMap<>();
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
                case DashboardRepository.ACTIVITY_DAY -> {
                    if (row.day() != null) {
                        countsByDay.merge(row.day(), orZero(row.dayTotal()), Long::sum);
                    }
                }
                case DashboardRepository.PRODUCT ->
                        productUses.merge(row.productName(), orZero(row.productUses()), Long::sum);
                case DashboardRepository.QUERY_TYPE ->
                        queryCounts.merge(row.queryType(), orZero(row.queryCount()), Long::sum);
                default -> log.warn("Seção desconhecida na mv_dashboard: {}", row.section());
            }
        }

        ActivityChart chart = chart(countsByDay, period);
        long periodTotal = chart.bars().stream().mapToLong(ChartBar::getCount).sum();

        return new DashboardDTO(users, companies, products, alerts, updatedAt,
                period, periodTotal, chart.bars(), chart.linePoints(), chart.max(),
                activities.stream().limit(ACTIVITY_SIZE).toList(),
                alertItems.stream().limit(LIST_SIZE).toList(),
                ranking(productUses),
                ranking(queryCounts));
    }

    /** Só entram no gráfico os dias do período escolhido; os dias sem alteração valem zero. */
    private ActivityChart chart(Map<LocalDate, Long> countsByDay, int period) {
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDay = today.minusDays(period - 1L);
        Map<LocalDate, Long> inPeriod = new HashMap<>();
        countsByDay.forEach((day, count) -> {
            if (!day.isBefore(firstDay) && !day.isAfter(today)) {
                inPeriod.put(day, count);
            }
        });
        return new ActivityChart(inPeriod, today, period);
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

    private static long orZero(Long value) {
        return value == null ? 0 : value;
    }

    /**
     * Separa o tipo, o nome e o status do texto do alerta para a tela mostrar o selo e o link da lista de origem.
     */
    private static AlertItem alertOf(String text) {
        Matcher matcher = text == null ? null : ALERT_TEXT.matcher(text);
        if (matcher != null && matcher.matches()) {
            try {
                Status status = Status.valueOf(matcher.group(3));
                boolean user = "Usuário".equals(matcher.group(2));
                return new AlertItem(matcher.group(2), matcher.group(1), status, user ? "/users" : "/companies");
            } catch (IllegalArgumentException e) {
                // status que o sistema não conhece: cai no texto inteiro, abaixo
            }
        }
        return new AlertItem(null, text == null ? "" : text, null, null);
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
