package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.dto.DashboardDTO.Dot;
import com.quimia.quimiawebcrud.dto.DashboardDTO.LineChart;
import com.quimia.quimiawebcrud.dto.DashboardDTO.Tick;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gráfico de atividade: uma linha suave para o período atual (com a área abaixo preenchida) e outra, tracejada,
 * para o período anterior de mesmo tamanho. Tudo em coordenadas de um desenho SVG, para a tela não precisar de
 * JavaScript nem de estilos inline.
 */
final class ActivityChart {

    static final int LEFT = 44;
    static final int RIGHT = 628;
    static final int TOP = 16;
    static final int BOTTOM = 250;
    private static final int MAX_LABELS = 8;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ActivityChart() {
    }

    /**
     * @param current  quantidade de registros por dia no período atual (dias sem registro valem zero)
     * @param previous o mesmo para o período imediatamente anterior
     * @param lastDay  último dia do gráfico (hoje)
     * @param days     quantidade de dias de cada período (7, 30 ou 90)
     */
    static LineChart build(Map<LocalDate, Long> current, Map<LocalDate, Long> previous, LocalDate lastDay, int days) {
        long[] now = new long[days];
        long[] before = new long[days];
        long highest = 0;
        long currentTotal = 0;
        long previousTotal = 0;
        for (int i = 0; i < days; i++) {
            LocalDate day = lastDay.minusDays(days - 1L - i);
            now[i] = current.getOrDefault(day, 0L);
            before[i] = previous.getOrDefault(day.minusDays(days), 0L);
            highest = Math.max(highest, Math.max(now[i], before[i]));
            currentTotal += now[i];
            previousTotal += before[i];
        }
        int step = ChartScale.niceStep(highest);
        int max = step * ChartScale.GRID_LINES;

        double[][] nowPoints = new double[days][2];
        double[][] beforePoints = new double[days][2];
        List<Dot> dots = new ArrayList<>();
        List<Tick> xLabels = new ArrayList<>();
        int labelStep = (int) Math.ceil(days / (double) MAX_LABELS);
        for (int i = 0; i < days; i++) {
            double x = LEFT + i * (RIGHT - LEFT) / (double) (days - 1);
            nowPoints[i] = new double[]{x, yOf(now[i], max)};
            beforePoints[i] = new double[]{x, yOf(before[i], max)};
            LocalDate day = lastDay.minusDays(days - 1L - i);
            dots.add(new Dot(ChartScale.number(x), ChartScale.number(nowPoints[i][1]),
                    day.format(FULL) + ": " + now[i] + (now[i] == 1 ? " registro" : " registros")
                            + " (período anterior: " + before[i] + ")"));
            if ((days - 1 - i) % labelStep == 0) {
                xLabels.add(new Tick((int) Math.round(x), labelOf(day, days)));
            }
        }

        String line = smoothPath(nowPoints);
        String area = line + " L" + ChartScale.number(nowPoints[days - 1][0]) + "," + BOTTOM
                + " L" + ChartScale.number(nowPoints[0][0]) + "," + BOTTOM + " Z";
        return new LineChart(ChartScale.yTicks(step, TOP, BOTTOM), xLabels, line, area, smoothPath(beforePoints), dots,
                max, currentTotal, previousTotal, ChartScale.trendText(currentTotal, previousTotal),
                ChartScale.trend(currentTotal, previousTotal));
    }

    private static double yOf(long value, int max) {
        return BOTTOM - value * (BOTTOM - TOP) / (double) max;
    }

    /**
     * Curva suave que passa por todos os pontos (Catmull-Rom convertida em curvas de Bézier). Os pontos de controle
     * ficam dentro da área do gráfico, para a linha nunca descer abaixo do zero.
     */
    static String smoothPath(double[][] p) {
        StringBuilder path = new StringBuilder("M").append(ChartScale.number(p[0][0])).append(',')
                .append(ChartScale.number(p[0][1]));
        for (int i = 0; i < p.length - 1; i++) {
            double[] before = p[Math.max(i - 1, 0)];
            double[] from = p[i];
            double[] to = p[i + 1];
            double[] after = p[Math.min(i + 2, p.length - 1)];
            double c1x = from[0] + (to[0] - before[0]) / 6;
            double c1y = clamp(from[1] + (to[1] - before[1]) / 6);
            double c2x = to[0] - (after[0] - from[0]) / 6;
            double c2y = clamp(to[1] - (after[1] - from[1]) / 6);
            path.append(" C").append(ChartScale.number(c1x)).append(',').append(ChartScale.number(c1y))
                    .append(' ').append(ChartScale.number(c2x)).append(',').append(ChartScale.number(c2y))
                    .append(' ').append(ChartScale.number(to[0])).append(',').append(ChartScale.number(to[1]));
        }
        return path.toString();
    }

    private static double clamp(double y) {
        return Math.max(TOP, Math.min(BOTTOM, y));
    }

    private static String labelOf(LocalDate day, int days) {
        if (days <= 7) {
            String name = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR);
            return name.substring(0, 1).toUpperCase(PT_BR) + name.substring(1).replace(".", "");
        }
        return day.format(DateTimeFormatter.ofPattern("dd/MM"));
    }
}
