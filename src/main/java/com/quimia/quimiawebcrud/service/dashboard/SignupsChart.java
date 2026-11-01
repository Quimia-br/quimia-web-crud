package com.quimia.quimiawebcrud.service.dashboard;

import com.quimia.quimiawebcrud.dto.response.DashboardDTO.Bar;
import com.quimia.quimiawebcrud.dto.response.DashboardDTO.MonthGroup;
import com.quimia.quimiawebcrud.dto.response.DashboardDTO.MonthlyChart;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
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

    private SignupsChart() {
    }

    /**
     * @param users     cadastros de usuários por mês (meses fora dos últimos {@value #MONTHS} são ignorados)
     * @param companies o mesmo para empresas
     * @param lastMonth último mês do gráfico (o atual)
     * @param texts     os textos do gráfico no idioma do pedido
     */
    static MonthlyChart build(Map<YearMonth, Long> users, Map<YearMonth, Long> companies, YearMonth lastMonth,
                              DashboardTexts texts) {
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
            groups.add(new MonthGroup(labelOf(month, texts), center,
                    bar(center - BAR_GAP / 2 - BAR_WIDTH, users.getOrDefault(month, 0L), max, texts.get("dash.signups.usersWord"), month, texts),
                    bar(center + BAR_GAP / 2, companies.getOrDefault(month, 0L), max, texts.get("dash.signups.companiesWord"), month, texts)));
        }
        return new MonthlyChart(ChartScale.yTicks(step, TOP, BOTTOM), groups, max);
    }

    private static Bar bar(int x, long count, int max, String what, YearMonth month, DashboardTexts texts) {
        int height = (int) Math.round(count * (BOTTOM - TOP) / (double) max);
        return new Bar(x, BOTTOM - height, BAR_WIDTH, height, count,
                labelOf(month, texts) + "/" + month.getYear() + ": " + count + " " + what);
    }

    private static String labelOf(YearMonth month, DashboardTexts texts) {
        String name = month.getMonth().getDisplayName(TextStyle.SHORT, texts.locale()).replace(".", "");
        return name.substring(0, 1).toUpperCase(texts.locale()) + name.substring(1);
    }
}
