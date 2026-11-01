package com.quimia.quimiawebcrud.service.dashboard;

import com.quimia.quimiawebcrud.dto.DashboardDTO.Tick;
import com.quimia.quimiawebcrud.dto.DashboardDTO.Trend;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Escala dos gráficos: números "redondos" nas linhas de grade e o texto da variação entre dois períodos.
 */
final class ChartScale {

    static final int GRID_LINES = 4;

    private ChartScale() {
    }

    /** Menor passo (1, 2, 5, 10, 20, 50...) para o qual quatro passos cobrem o maior valor. */
    static int niceStep(long highest) {
        long magnitude = 1;
        while (true) {
            for (int base : new int[]{1, 2, 5}) {
                long step = base * magnitude;
                if (step * GRID_LINES >= highest) {
                    return (int) step;
                }
            }
            magnitude *= 10;
        }
    }

    /** Linhas de grade do eixo vertical, de baixo (zero) para cima. */
    static List<Tick> yTicks(int step, int top, int bottom) {
        List<Tick> ticks = new ArrayList<>();
        for (int k = 0; k <= GRID_LINES; k++) {
            ticks.add(new Tick(bottom - k * (bottom - top) / GRID_LINES, String.valueOf(k * step)));
        }
        return ticks;
    }

    static Trend trend(long current, long previous) {
        if (current > previous) {
            return Trend.UP;
        }
        return current < previous ? Trend.DOWN : Trend.FLAT;
    }

    /** "+12%", "-8%", "0%" ou "novo" (quando antes não havia nada). */
    static String trendText(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? "novo" : "0%";
        }
        long percent = Math.round((current - previous) * 100.0 / previous);
        return (percent > 0 ? "+" : "") + percent + "%";
    }

    /** Número com uma casa decimal e ponto, o formato que o SVG espera. */
    static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
