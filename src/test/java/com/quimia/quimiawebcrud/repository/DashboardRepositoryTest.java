package com.quimia.quimiawebcrud.repository;

import com.quimia.quimiawebcrud.repository.DashboardRepository.Row;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sem banco aqui: confere que a consulta pede as colunas da mv_dashboard e que cada linha vira um Row, aceitando
 * os tipos que o driver devolve para colunas de data com e sem fuso.
 */
class DashboardRepositoryTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final DashboardRepository repository = new DashboardRepository(entityManager);

    @Test
    void readsEveryColumnOfTheViewInOrder() {
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        repository.findAll();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture());
        for (String column : new String[]{"secao", "contagem_usuarios", "contagem_produtos", "contagem_empresas",
                "contagem_alertas", "admin", "acao", "data_edicao", "tabela_afetada", "alerta", "total_usos",
                "nome_produto", "tipo_contagem", "nome_tipo", "data_dia", "total_dia", "atualizado_em"}) {
            assertTrue(sql.getValue().contains(column), "falta a coluna " + column);
        }
        assertTrue(sql.getValue().contains("from mv_dashboard order by id"));
    }

    @Test
    void refreshRunsTheConcurrentRefreshOfTheView() {
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(query);

        repository.refresh();

        verify(entityManager).createNativeQuery("refresh materialized view concurrently mv_dashboard");
        verify(query).executeUpdate();
    }

    @Test
    void mapsTotalsAndActivityRowsWhateverDateTypeTheDriverReturns() {
        Timestamp editedAt = Timestamp.valueOf(LocalDateTime.of(2026, 10, 20, 9, 30));
        OffsetDateTime updatedAt = OffsetDateTime.of(2026, 10, 20, 15, 0, 0, 0, ZoneOffset.UTC);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(
                new Object[]{"totais", 3L, 5L, 2L, 1L, null, null, null, null, null, null, null, null, null, null, null, updatedAt},
                new Object[]{"atividade", null, null, null, null, "Adm", "DELETE", editedAt, "produtos", null, null, null, null, null,
                        null, null, LocalDateTime.of(2026, 10, 20, 12, 0)},
                new Object[]{"atividade_dia", null, null, null, null, null, null, null, null, null, null, null, null, null,
                        java.sql.Date.valueOf(LocalDate.of(2026, 10, 19)), 4L, updatedAt}));

        List<Row> rows = repository.findAll();

        assertEquals(3, rows.size());
        Row totals = rows.get(0);
        assertEquals("totais", totals.section());
        assertEquals(3L, totals.users());
        assertEquals(1L, totals.alerts());
        assertNull(totals.admin());
        assertEquals(updatedAt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), totals.updatedAt());
        Row activity = rows.get(1);
        assertEquals("Adm", activity.admin());
        assertEquals("DELETE", activity.action());
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 30), activity.editDate());
        assertEquals(LocalDateTime.of(2026, 10, 20, 12, 0), activity.updatedAt());
        Row day = rows.get(2);
        assertEquals("atividade_dia", day.section());
        assertEquals(LocalDate.of(2026, 10, 19), day.day());
        assertEquals(4L, day.dayTotal());
    }
}
