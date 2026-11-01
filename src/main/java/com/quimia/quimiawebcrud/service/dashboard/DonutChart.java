package com.quimia.quimiawebcrud.service.dashboard;

import com.quimia.quimiawebcrud.dto.response.DashboardDTO.Donut;
import com.quimia.quimiawebcrud.dto.response.DashboardDTO.Slice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rosca de proporções (as cinco maiores categorias e "Outros"). Cada fatia é um círculo com traço e deslocamento
 * calculados aqui: o SVG só desenha, sem JavaScript.
 */
final class DonutChart {

    static final int RADIUS = 52;
    private static final double CIRCUMFERENCE = 2 * Math.PI * RADIUS;
    private static final double SEPARATION = 2;
    private static final int MAX_SLICES = 5;

    private DonutChart() {
    }

    static Donut build(Map<String, Long> counts, DashboardTexts texts) {
        List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .toList();
        long total = sorted.stream().mapToLong(Map.Entry::getValue).sum();
        if (total == 0) {
            return new Donut(0, List.of());
        }

        List<Map.Entry<String, Long>> shown = new ArrayList<>(sorted.stream().limit(MAX_SLICES).toList());
        long others = sorted.stream().skip(MAX_SLICES).mapToLong(Map.Entry::getValue).sum();
        if (others > 0) {
            shown.add(Map.entry(texts.get("dash.other"), others));
        }

        List<Slice> slices = new ArrayList<>();
        double start = 0;
        int index = 1;
        for (Map.Entry<String, Long> entry : shown) {
            double length = entry.getValue() * CIRCUMFERENCE / total;
            double dash = Math.max(length - SEPARATION, 0.5);
            slices.add(new Slice(entry.getKey(), entry.getValue(), (int) Math.round(entry.getValue() * 100.0 / total),
                    index++, number(dash), number(CIRCUMFERENCE - dash), number(-start)));
            start += length;
        }
        return new Donut(total, slices);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
