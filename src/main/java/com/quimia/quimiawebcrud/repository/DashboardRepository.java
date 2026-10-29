package com.quimia.quimiawebcrud.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * Lê a view materializada mv_dashboard, que já traz tudo o que a tela inicial mostra: totais, alertas, últimas
 * ações dos administradores e os rankings. A view é do banco (não é uma entidade), então a leitura é nativa.
 * Cada linha pertence a uma seção (coluna {@code secao}); as colunas das outras seções vêm vazias.
 */
@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardRepository {

    public static final String TOTALS = "totais";
    public static final String ALERT = "alerta";
    public static final String ACTIVITY = "atividade";
    public static final String PRODUCT = "produto";
    public static final String QUERY_TYPE = "tipo_consulta";
    public static final String USAGE_ACTIVITY = "atividade_uso";
    public static final String USAGE_DAY = "uso_dia";
    public static final String USER_SIGNUP_DAY = "cadastro_usuario_dia";
    public static final String COMPANY_SIGNUP_DAY = "cadastro_empresa_dia";
    public static final String USER_SIGNUP_MONTH = "cadastro_usuario_mes";
    public static final String COMPANY_SIGNUP_MONTH = "cadastro_empresa_mes";

    private static final String SELECT = "select secao, contagem_usuarios, contagem_produtos, contagem_empresas, "
            + "contagem_alertas, admin, acao, data_edicao, tabela_afetada, alerta, total_usos, nome_produto, "
            + "tipo_contagem, nome_tipo, data_dia, total_dia, atualizado_em from mv_dashboard order by id";

    /** CONCURRENTLY: quem estiver abrindo o dashboard continua lendo a versão antiga até a nova ficar pronta. */
    private static final String REFRESH = "refresh materialized view concurrently mv_dashboard";

    /** Uma linha da view; só as colunas da própria {@code section} vêm preenchidas. */
    public record Row(String section, Long users, Long products, Long companies, Long alerts,
                      String admin, String action, LocalDateTime editDate, String affectedTable,
                      String alert, Long productUses, String productName, Long queryCount, String queryType,
                      LocalDate day, Long dayTotal, LocalDateTime updatedAt) {
    }

    private final EntityManager entityManager;

    /** Todas as linhas, na ordem da própria view (por seção; dentro de cada uma, as mais relevantes primeiro). */
    public List<Row> findAll() {
        return entityManager.createNativeQuery(SELECT).getResultList().stream()
                .map(row -> toRow((Object[]) row))
                .toList();
    }

    /** Recalcula a view com os dados de agora. Precisa do índice único da view e de o usuário do banco ser o dono dela. */
    @Transactional
    public void refresh() {
        entityManager.createNativeQuery(REFRESH).executeUpdate();
    }

    private static Row toRow(Object[] c) {
        return new Row((String) c[0], number(c[1]), number(c[2]), number(c[3]), number(c[4]),
                (String) c[5], (String) c[6], dateTime(c[7]), (String) c[8],
                (String) c[9], number(c[10]), (String) c[11], number(c[12]), (String) c[13],
                date(c[14]), number(c[15]), dateTime(c[16]));
    }

    private static Long number(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    /** Coluna do tipo date (data_dia): o driver pode devolver java.sql.Date ou LocalDate. */
    private static LocalDate date(Object value) {
        return switch (value) {
            case null -> null;
            case LocalDate local -> local;
            case java.sql.Date sql -> sql.toLocalDate();
            case Timestamp timestamp -> timestamp.toLocalDateTime().toLocalDate();
            default -> throw new IllegalStateException("Tipo de data inesperado na mv_dashboard: " + value.getClass());
        };
    }

    /** O driver devolve tipos diferentes conforme a coluna ser com ou sem fuso (data_edicao, atualizado_em). */
    private static LocalDateTime dateTime(Object value) {
        ZoneId zone = ZoneId.systemDefault();
        return switch (value) {
            case null -> null;
            case LocalDateTime local -> local;
            case Timestamp timestamp -> timestamp.toLocalDateTime();
            case OffsetDateTime offset -> offset.atZoneSameInstant(zone).toLocalDateTime();
            case ZonedDateTime zoned -> zoned.withZoneSameInstant(zone).toLocalDateTime();
            case Instant instant -> LocalDateTime.ofInstant(instant, zone);
            case Date date -> LocalDateTime.ofInstant(date.toInstant(), zone);
            default -> throw new IllegalStateException("Tipo de data inesperado na mv_dashboard: " + value.getClass());
        };
    }
}
