package com.quimia.quimiawebcrud.service.removal;

import com.quimia.quimiawebcrud.service.crud.AdminService;
import com.quimia.quimiawebcrud.service.crud.CompanyService;
import com.quimia.quimiawebcrud.service.crud.DisposalService;
import com.quimia.quimiawebcrud.service.crud.HistoryTypeService;
import com.quimia.quimiawebcrud.service.crud.ProductService;
import com.quimia.quimiawebcrud.service.crud.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificação contra o banco REAL (o do arquivo .env): exclui um registro de cada tipo, restaura e confere que
 * as linhas voltaram idênticas. Só roda com QUIMIA_REAL_DB_CHECK=1 e termina sempre desfazendo tudo
 * (a transação do teste é revertida, então o banco não muda).
 */
@EnabledIfEnvironmentVariable(named = "QUIMIA_REAL_DB_CHECK", matches = "1")
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.jpa.hibernate.ddl-auto=none"})
@Transactional
class RealDatabaseRestoreCheckTest {

    @PersistenceContext
    EntityManager entityManager;
    @Autowired
    UserService userService;
    @Autowired
    CompanyService companyService;
    @Autowired
    ProductService productService;
    @Autowired
    HistoryTypeService historyTypeService;
    @Autowired
    DisposalService disposalService;
    @Autowired
    AdminService adminService;
    @Autowired
    AdminLogService adminLogService;
    @Autowired
    RecordRestoreService restoreService;

    private Number scalar(String sql, Object... params) {
        var query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) {
            query.setParameter("p" + i, params[i]);
        }
        List<?> result = query.getResultList();
        return result.isEmpty() ? null : (Number) result.get(0);
    }

    private List<Long> counts(List<RecordRemovalService.Part> parts, Integer id) {
        List<Long> counts = new ArrayList<>();
        for (RecordRemovalService.Part part : parts) {
            counts.add(((Number) entityManager.createNativeQuery(
                    "select count(*) from " + part.table() + " where " + part.where())
                    .setParameter("id", id).getSingleResult()).longValue());
        }
        return counts;
    }

    private JsonNode row(RecordRemovalService.Part record, Integer id) {
        String json = (String) entityManager.createNativeQuery(
                "select cast(to_jsonb(t) as text) from " + record.table() + " t where " + record.where())
                .setParameter("id", id).getSingleResult();
        ObjectNode node = (ObjectNode) RecordRemovalService.JSON.readTree(json);
        node.remove("url_foto");
        return node;
    }

    private void roundTrip(String what, List<RecordRemovalService.Part> parts, Integer id, Consumer<Integer> delete) {
        RecordRemovalService.Part record = parts.get(0);
        List<Long> before = counts(parts, id);
        JsonNode rowBefore = row(record, id);
        System.out.println("CHECK " + what + " #" + id + " linhas por tabela " + before);

        delete.accept(id);
        assertTrue(counts(parts, id).stream().allMatch(c -> c == 0), what + ": tudo apagado");

        Number logId = scalar("select max(id_admin_log_edicao) from admin_log_edicoes where tabela_afetada = '"
                + record.table() + "' and id_registro = " + id);
        assertNotNull(logId, what + ": a exclusão foi para o log");
        assertTrue(adminLogService.restorableIds().contains(logId.intValue()), what + ": a seta aparece");

        restoreService.restore(logId.intValue());

        assertEquals(before, counts(parts, id), what + ": mesmas linhas de volta");
        assertEquals(rowBefore, row(record, id), what + ": o registro voltou idêntico");
        assertFalse(adminLogService.restorableIds().contains(logId.intValue()), what + ": a seta some depois de restaurar");
    }

    @Test
    void deleteThenRestoreBringsEveryRecordTypeBackIdenticalAndTheDatabaseIsLeftUntouched() {
        String adminEmail = (String) entityManager.createNativeQuery("select email from admins order by id_admin limit 1")
                .getSingleResult();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(adminEmail, "x", "ROLE_ADMIN"));
        try {
            Number user = scalar("select id_usuario from usuarios order by "
                    + "(select count(*) from historicos h where h.id_usuario = usuarios.id_usuario) desc limit 1");
            if (user != null) {
                roundTrip("usuário", RecordRemovalService.USER_PARTS, user.intValue(), userService::delete);
            }
            Number company = scalar("select id_usuario_empresa from usuarios_empresas order by "
                    + "(select count(*) from produtos p where p.id_usuario_empresa = usuarios_empresas.id_usuario_empresa) desc limit 1");
            if (company != null) {
                roundTrip("empresa", RecordRemovalService.COMPANY_PARTS, company.intValue(), companyService::delete);
            }
            Number product = scalar("select id_produto from produtos order by "
                    + "(select count(*) from produtos_usuarios u where u.id_produto = produtos.id_produto) desc limit 1");
            if (product != null) {
                roundTrip("produto", RecordRemovalService.PRODUCT_PARTS, product.intValue(), productService::delete);
            }
            Number type = scalar("select id_tipo_historico from tipos_historicos order by "
                    + "(select count(*) from historicos h where h.id_tipo_historico = tipos_historicos.id_tipo_historico) desc limit 1");
            if (type != null) {
                roundTrip("tipo de histórico", RecordRemovalService.HISTORY_TYPE_PARTS, type.intValue(), historyTypeService::delete);
            }
            Number disposal = scalar("select id_descarte_fds from descartes_fds limit 1");
            if (disposal != null) {
                roundTrip("descarte", RecordRemovalService.DISPOSAL_PARTS, disposal.intValue(), disposalService::delete);
            }
            Number otherAdmin = scalar("select id_admin from admins where email <> '" + adminEmail.replace("'", "''") + "' limit 1");
            if (otherAdmin != null) {
                roundTrip("administrador", RecordRemovalService.ADMIN_PARTS, otherAdmin.intValue(), adminService::delete);
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
