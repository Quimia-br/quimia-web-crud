package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.DashboardDTO.ChartBar;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calcula as medidas do gráfico de atividade: uma coluna por dia e uma linha ligando o topo das colunas.
 * Tudo em coordenadas de um desenho SVG, para a tela não precisar de JavaScript nem de estilos inline.
 */
final class ActivityChart {

    static final int PLOT_LEFT = 40;
    static final int PLOT_WIDTH = 580;
    static final int PLOT_TOP = 20;
    static final int PLOT_HEIGHT = 180;
    private static final int MAX_BAR_WIDTH = 40;
    private static final int MAX_LABELS = 8;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final List<ChartBar> bars = new ArrayList<>();
    private final StringBuilder linePoints = new StringBuilder();
    private final int max;

    /**
     * @param countsByDay quantidade de registros por dia (dias sem registro valem zero)
     * @param lastDay     último dia do gráfico (hoje)
     * @param days        quantidade de dias mostrados
     */
    ActivityChart(Map<LocalDate, Long> countsByDay, LocalDate lastDay, int days) {
        long highest = 0;
        for (long value : countsByDay.values()) {
            highest = Math.max(highest, value);
        }
        this.max = (int) Math.max(highest, 1);
        int slot = PLOT_WIDTH / days;
        int barWidth = Math.max(2, Math.min(MAX_BAR_WIDTH, (int) (slot * 0.6)));
        int labelStep = (int) Math.ceil(days / (double) MAX_LABELS);
        for (int i = 0; i < days; i++) {
            LocalDate day = lastDay.minusDays(days - 1L - i);
            long count = countsByDay.getOrDefault(day, 0L);
            int height = (int) Math.round(count * (double) PLOT_HEIGHT / max);
            int x = PLOT_LEFT + i * slot + (slot - barWidth) / 2;
            int y = PLOT_TOP + PLOT_HEIGHT - height;
            boolean showLabel = (days - 1 - i) % labelStep == 0;
            bars.add(new ChartBar(labelOf(day, days), count, x, y, barWidth, height, showLabel, x + barWidth / 2,
                    day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ": " + count));
            if (i > 0) {
                linePoints.append(' ');
            }
            linePoints.append(x + barWidth / 2).append(',').append(y);
        }
    }

    List<ChartBar> bars() {
        return bars;
    }

    String linePoints() {
        return linePoints.toString();
    }

    int max() {
        return max;
    }

    private static String labelOf(LocalDate day, int days) {
        if (days <= 7) {
            String name = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR);
            return name.substring(0, 1).toUpperCase(PT_BR) + name.substring(1).replace(".", "");
        }
        return day.format(DateTimeFormatter.ofPattern("dd/MM"));
    }
}
