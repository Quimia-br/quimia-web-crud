package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tudo o que a tela inicial (dashboard) mostra.
 */
@Getter
@AllArgsConstructor
public class DashboardDTO {

    private final long totalUsers;
    private final long totalCompanies;
    private final long totalProducts;
    private final long totalAlerts;
    private final int periodDays;
    private final long periodTotal;
    private final List<ChartBar> bars;
    private final String linePoints;
    private final int chartMax;
    private final List<ActivityItem> latestActivities;
    private final List<AlertItem> recentAlerts;
    private final List<RankItem> topProducts;
    private final List<RankItem> topQueries;

    /**
     * Uma coluna do gráfico de atividade (um dia), com as medidas já calculadas para o desenho em SVG.
     */
    @Getter
    @AllArgsConstructor
    public static class ChartBar {
        private final String label;
        private final long count;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final boolean showLabel;
        private final int labelX;
        private final String title;
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
     * Uma conta que pede atenção (pendente ou bloqueada).
     */
    @Getter
    @AllArgsConstructor
    public static class AlertItem {
        private final String kind;
        private final String name;
        private final Status status;
        private final LocalDateTime date;
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
