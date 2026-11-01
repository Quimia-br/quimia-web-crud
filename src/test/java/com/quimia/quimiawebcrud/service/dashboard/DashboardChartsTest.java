package com.quimia.quimiawebcrud.service.dashboard;

import com.quimia.quimiawebcrud.dto.DashboardDTO.Donut;
import com.quimia.quimiawebcrud.dto.DashboardDTO.Slice;
import com.quimia.quimiawebcrud.dto.DashboardDTO.Trend;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contas dos gráficos: escala "redonda", variação entre períodos, fatias da rosca e curva suave.
 */
class DashboardChartsTest {

    private static final DashboardTexts TEXTS = new DashboardTexts(com.quimia.quimiawebcrud.TestMessages.source(), com.quimia.quimiawebcrud.TestMessages.PT);

    @Test
    void scaleUsesRoundStepsSoFourGridLinesAlwaysCoverTheHighestValue() {
        assertEquals(1, ChartScale.niceStep(0));
        assertEquals(1, ChartScale.niceStep(4));
        assertEquals(2, ChartScale.niceStep(5));
        assertEquals(2, ChartScale.niceStep(8));
        assertEquals(5, ChartScale.niceStep(9));
        assertEquals(5, ChartScale.niceStep(20));
        assertEquals(10, ChartScale.niceStep(21));
        assertEquals(50, ChartScale.niceStep(101));
        assertEquals(500, ChartScale.niceStep(1000), "os passos são 1, 2 e 5 vezes uma potência de 10");
    }

    @Test
    void trendTextAndDirectionComparePeriods() {
        assertEquals("+20%", ChartScale.trendText(12, 10, "novo"));
        assertEquals("-20%", ChartScale.trendText(8, 10, "novo"));
        assertEquals("0%", ChartScale.trendText(5, 5, "novo"));
        assertEquals("0%", ChartScale.trendText(0, 0, "novo"));
        assertEquals("novo", ChartScale.trendText(3, 0, "novo"), "antes não havia nada, não dá para calcular a porcentagem");
        assertEquals(Trend.UP, ChartScale.trend(3, 0));
        assertEquals(Trend.DOWN, ChartScale.trend(1, 2));
        assertEquals(Trend.FLAT, ChartScale.trend(2, 2));
    }

    @Test
    void donutSlicesAddUpToTheWholeCircleAndFollowEachOther() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("B", 3L);
        counts.put("A", 6L);
        counts.put("C", 1L);
        counts.put("Zero", 0L);

        Donut donut = DonutChart.build(counts, TEXTS);

        assertEquals(10, donut.getTotal());
        assertEquals(3, donut.getSlices().size(), "categoria sem registros não aparece");
        assertEquals("A", donut.getSlices().get(0).getName());
        assertEquals(60, donut.getSlices().get(0).getPercent());
        assertEquals(30, donut.getSlices().get(1).getPercent());
        assertEquals(10, donut.getSlices().get(2).getPercent());
        assertEquals("0.00", donut.getSlices().get(0).getOffset().replace("-", ""), "a primeira fatia começa no topo");
        double circumference = 2 * Math.PI * DonutChart.RADIUS;
        for (Slice slice : donut.getSlices()) {
            assertEquals(circumference, Double.parseDouble(slice.getDash()) + Double.parseDouble(slice.getGap()), 0.02);
        }
        assertEquals(-0.6 * circumference, Double.parseDouble(donut.getSlices().get(1).getOffset()), 0.05,
                "a segunda fatia começa onde a primeira termina");
    }

    @Test
    void donutGroupsTheSmallestCategoriesAsOthersAfterFiveSlices() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int i = 1; i <= 8; i++) {
            counts.put("Tipo " + i, (long) (10 - i));
        }

        Donut donut = DonutChart.build(counts, TEXTS);

        assertEquals(6, donut.getSlices().size());
        Slice others = donut.getSlices().get(5);
        assertEquals("Outros", others.getName());
        assertEquals(9, others.getCount(), "Tipo 6, 7 e 8 somam 4+3+2");
        assertEquals(6, others.getColorIndex());
    }

    @Test
    void emptyDonutHasNoSlices() {
        Donut donut = DonutChart.build(Map.of(), TEXTS);

        assertEquals(0, donut.getTotal());
        assertTrue(donut.getSlices().isEmpty());
    }

    @Test
    void smoothCurveKeepsEveryControlPointInsideThePlotSoItNeverDipsBelowZero() {
        double[][] spiky = new double[][]{{44, 250}, {120, 16}, {196, 250}, {272, 16}, {348, 250}};

        String path = ActivityChart.smoothPath(spiky);

        assertTrue(path.startsWith("M44.0,250.0 C"));
        for (String curve : path.substring(1).split(" C")) {
            for (String pair : curve.replace("M", "").trim().split(" ")) {
                String[] xy = pair.split(",");
                if (xy.length == 2) {
                    double y = Double.parseDouble(xy[1]);
                    assertTrue(y >= ActivityChart.TOP && y <= ActivityChart.BOTTOM, "y fora da área: " + y);
                }
            }
        }
        assertEquals(4, path.split(" C").length - 1, "uma curva entre cada par de pontos");
    }
}
