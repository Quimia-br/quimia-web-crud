package com.quimia.quimiawebcrud.service.dashboard;

import com.quimia.quimiawebcrud.dto.DashboardDTO.Bar;
import com.quimia.quimiawebcrud.dto.DashboardDTO.MonthGroup;
import com.quimia.quimiawebcrud.dto.DashboardDTO.MonthlyChart;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Barras de cadastros por mês (usuários e empresas lado a lado) nos últimos meses, em coordenadas de SVG.
 */
final class SignupsChart {

    static final int MONTHS = 6;
    static final int LEFT = 44;
    static final int RIGHT = 628;
    static final int TOP = 16;
    static final int BOTTOM = 250;
    private static final int BAR_WIDTH = 22;
    private static final int BAR_GAP = 6;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private SignupsChart() {
    }

    /**
     * @param users     cadastros de usuários por mês (meses fora dos últimos {@value #MONTHS} são ignorados)
     * @param companies o mesmo para empresas
     * @param lastMonth último mês do gráfico (o atual)
     */
    static MonthlyChart build(Map<YearMonth, Long> users, Map<YearMonth, Long> companies, YearMonth lastMonth) {
        List<YearMonth> months = new ArrayList<>();
        long highest = 0;
        for (int i = MONTHS - 1; i >= 0; i--) {
            YearMonth month = lastMonth.minusMonths(i);
            months.add(month);
            highest = Math.max(highest, Math.max(users.getOrDefault(month, 0L), companies.getOrDefault(month, 0L)));
        }
        int step = ChartScale.niceStep(highest);
        int max = step * ChartScale.GRID_LINES;

        int slot = (RIGHT - LEFT) / MONTHS;
        List<MonthGroup> groups = new ArrayList<>();
        for (int i = 0; i < months.size(); i++) {
            YearMonth month = months.get(i);
            int center = LEFT + i * slot + slot / 2;
            groups.add(new MonthGroup(labelOf(month), center,
                    bar(center - BAR_GAP / 2 - BAR_WIDTH, users.getOrDefault(month, 0L), max, "usuários", month),
                    bar(center + BAR_GAP / 2, companies.getOrDefault(month, 0L), max, "empresas", month)));
        }
        return new MonthlyChart(ChartScale.yTicks(step, TOP, BOTTOM), groups, max);
    }

    private static Bar bar(int x, long count, int max, String what, YearMonth month) {
        int height = (int) Math.round(count * (BOTTOM - TOP) / (double) max);
        return new Bar(x, BOTTOM - height, BAR_WIDTH, height, count,
                labelOf(month) + "/" + month.getYear() + ": " + count + " " + what);
    }

    private static String labelOf(YearMonth month) {
        String name = month.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
        return name.substring(0, 1).toUpperCase(PT_BR) + name.substring(1);
    }
}
