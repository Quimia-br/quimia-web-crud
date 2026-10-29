package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tudo o que a tela inicial (dashboard) mostra. Os gráficos já vêm com as medidas calculadas para o desenho em SVG,
 * para a tela não precisar de JavaScript nem de estilos inline.
 */
@Getter
@AllArgsConstructor
public class DashboardDTO {

    private final String todayLabel;
    private final LocalDateTime updatedAt;
    private final int periodDays;
    private final long totalUsers;
    private final long totalCompanies;
    private final long totalProducts;
    private final long totalAlerts;
    private final long periodTotal;
    private final List<Kpi> kpis;
    private final LineChart activity;
    private final Donut queriesByType;
    private final MonthlyChart signups;
    private final List<ActivityItem> latestActivities;
    private final List<AlertItem> recentAlerts;
    private final List<RankItem> topProducts;

    public enum Trend {
        UP, DOWN, FLAT
    }

    /**
     * Um cartão de número do topo: valor, legenda e a variação em relação ao período anterior.
     */
    @Getter
    @AllArgsConstructor
    public static class Kpi {
        private final String label;
        private final long value;
        private final String caption;
        private final String trendText;
        private final Trend trend;
        private final boolean highlight;
        private final String path;
    }

    /** Uma marca dos eixos: posição no desenho e o texto. */
    @Getter
    @AllArgsConstructor
    public static class Tick {
        private final int pos;
        private final String text;
    }

    /** Ponto do gráfico de linha, com o texto que aparece ao passar o mouse. */
    @Getter
    @AllArgsConstructor
    public static class Dot {
        private final String x;
        private final String y;
        private final String title;
    }

    /**
     * Gráfico de linhas suaves: o período atual (com área preenchida) contra o período anterior.
     */
    @Getter
    @AllArgsConstructor
    public static class LineChart {
        private final List<Tick> yTicks;
        private final List<Tick> xLabels;
        private final String currentLine;
        private final String currentArea;
        private final String previousLine;
        private final List<Dot> dots;
        private final int max;
        private final long currentTotal;
        private final long previousTotal;
        private final String trendText;
        private final Trend trend;
    }

    /** Uma fatia da rosca: a legenda e as medidas do arco (traço e deslocamento do círculo). */
    @Getter
    @AllArgsConstructor
    public static class Slice {
        private final String name;
        private final long count;
        private final int percent;
        private final int colorIndex;
        private final String dash;
        private final String gap;
        private final String offset;
    }

    @Getter
    @AllArgsConstructor
    public static class Donut {
        private final long total;
        private final List<Slice> slices;
    }

    /** Uma coluna do gráfico de barras mensal. */
    @Getter
    @AllArgsConstructor
    public static class Bar {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final long count;
        private final String title;
    }

    @Getter
    @AllArgsConstructor
    public static class MonthGroup {
        private final String label;
        private final int labelX;
        private final Bar users;
        private final Bar companies;
    }

    /**
     * Cadastros por mês: usuários e empresas lado a lado.
     */
    @Getter
    @AllArgsConstructor
    public static class MonthlyChart {
        private final List<Tick> yTicks;
        private final List<MonthGroup> months;
        private final int max;
    }

    /**
     * Um acontecimento recente: uso registrado por um usuário ou alteração feita por um administrador.
     */
    @Getter
    @AllArgsConstructor
    public static class ActivityItem {
        private final String text;
        private final LocalDateTime date;
    }

    /**
     * Uma conta que pede atenção (pendente ou bloqueada). Se o texto da view não tiver o formato esperado,
     * {@code kind}, {@code status} e {@code path} ficam nulos e {@code name} guarda o texto inteiro.
     */
    @Getter
    @AllArgsConstructor
    public static class AlertItem {
        private final String kind;
        private final String name;
        private final Status status;
        private final String path;
    }

    /**
     * Uma linha de ranking: nome, quantidade e o tamanho da barra (0 a 100).
     */
    @Getter
    @AllArgsConstructor
    public static class RankItem {
        private final String name;
        private final long count;
        private final int percent;
    }
}
