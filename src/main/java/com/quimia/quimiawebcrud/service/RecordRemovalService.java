package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.exception.InvalidDataException;
import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.Company;
import com.quimia.quimiawebcrud.model.Disposal;
import com.quimia.quimiawebcrud.model.HistoryType;
import com.quimia.quimiawebcrud.model.Model;
import com.quimia.quimiawebcrud.model.Product;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Exclusão de um registro junto com tudo que depende dele (o banco não deixa apagar quem ainda é referenciado).
 * <p>
 * Cada tipo tem um plano: a lista de tabelas afetadas, na ordem em que as linhas podem ser reinseridas
 * (o próprio registro primeiro, depois quem depende dele). Antes de apagar, uma cópia de todas as linhas é
 * guardada no log de edições, junto com o administrador que fez a exclusão; é essa cópia que permite restaurar.
 * As linhas são apagadas na ordem inversa. Tudo acontece em uma transação: se algo falhar, nada é apagado.
 */
@Service
@RequiredArgsConstructor
public class RecordRemovalService {

    /** Tabela afetada e quais das suas linhas pertencem ao registro excluído ({@code :id}). */
    record Part(String label, String table, String where) {
    }

    private static final String COPY_ROWS = "select cast(coalesce(jsonb_agg(to_jsonb(t)), cast('[]' as jsonb)) as text) "
            + "from %s t where %s";

    private static final String USER_SHELVES = "(select id_estante from estantes where id_usuario = :id)";
    private static final String USER_HISTORIES = "(select id_historico from historicos where id_usuario = :id "
            + "or id_estante in " + USER_SHELVES + ")";
    private static final String COMPANY_PRODUCTS = "(select id_produto from produtos where id_usuario_empresa = :id)";

    static final List<Part> USER_PARTS = List.of(
            new Part("usuário", "usuarios", "id_usuario = :id"),
            new Part("localizações", "localizacoes", "id_usuario = :id"),
            new Part("estantes", "estantes", "id_usuario = :id"),
            new Part("produtos do usuário", "produtos_usuarios", "id_usuario = :id"),
            new Part("produtos nas estantes", "produtos_estantes", "id_estante in " + USER_SHELVES),
            new Part("históricos", "historicos",
                    "id_usuario = :id or id_estante in " + USER_SHELVES),
            new Part("itens de histórico", "historicos_produtos",
                    "id_usuario = :id or id_historico in " + USER_HISTORIES));

    static final List<Part> COMPANY_PARTS = List.of(
            new Part("empresa", "usuarios_empresas", "id_usuario_empresa = :id"),
            new Part("produtos", "produtos", "id_usuario_empresa = :id"),
            new Part("fichas de descarte", "descartes_fds", "id_produto in " + COMPANY_PRODUCTS),
            new Part("produtos dos usuários", "produtos_usuarios", "id_produto in " + COMPANY_PRODUCTS),
            new Part("produtos nas estantes", "produtos_estantes", "id_produto in " + COMPANY_PRODUCTS),
            new Part("itens de histórico", "historicos_produtos", "id_produto in " + COMPANY_PRODUCTS));

    static final List<Part> PRODUCT_PARTS = List.of(
            new Part("produto", "produtos", "id_produto = :id"),
            new Part("fichas de descarte", "descartes_fds", "id_produto = :id"),
            new Part("produtos dos usuários", "produtos_usuarios", "id_produto = :id"),
            new Part("produtos nas estantes", "produtos_estantes", "id_produto = :id"),
            new Part("itens de histórico", "historicos_produtos", "id_produto = :id"));

    static final List<Part> HISTORY_TYPE_PARTS = List.of(
            new Part("tipo de histórico", "tipos_historicos", "id_tipo_historico = :id"),
            new Part("históricos", "historicos", "id_tipo_historico = :id"),
            new Part("itens de histórico", "historicos_produtos",
                    "id_historico in (select id_historico from historicos where id_tipo_historico = :id)"));

    static final List<Part> DISPOSAL_PARTS = List.of(
            new Part("ficha de descarte", "descartes_fds", "id_descarte_fds = :id"));

    static final List<Part> ADMIN_PARTS = List.of(
            new Part("administrador", "admins", "id_admin = :id"),
            new Part("registros do log", "admin_log_edicoes", "id_admin = :id"));

    /** Todos os planos, para os testes conferirem as tabelas e colunas contra o modelo. */
    static List<Part> allParts() {
        return Stream.of(USER_PARTS, COMPANY_PARTS, PRODUCT_PARTS, HISTORY_TYPE_PARTS, DISPOSAL_PARTS, ADMIN_PARTS)
                .flatMap(List::stream).toList();
    }

    static final JsonMapper JSON = JsonMapper.builder().build();

    private final EntityManager entityManager;
    private final CurrentAdmin currentAdmin;
    private final AdminLogRepository logs;

    @Transactional
    public void removeUser(User user) {
        remove(user, USER_PARTS, List.of(
                field("Nome", user.getUserName()), field("E-mail", user.getEmail()), field("Status", user.getStatus()),
                field("Nascimento", user.getBirthDate()), field("Cadastro", user.getSignUpDate()),
                field("Foto", user.getPhotoUrl())));
    }

    @Transactional
    public void removeCompany(Company company) {
        remove(company, COMPANY_PARTS, List.of(
                field("Nome", company.getName()), field("CNPJ", company.getCnpj()), field("E-mail", company.getEmail()),
                field("Porte", company.getSize()), field("Status", company.getStatus()),
                field("Cadastro", company.getSignUpDate()), field("Foto", company.getPhotoUrl())));
    }

    @Transactional
    public void removeProduct(Product product) {
        remove(product, PRODUCT_PARTS, List.of(
                field("Nome", product.getName()), field("CAS", product.getCasNumber()), field("Marca", product.getBrand()),
                field("Empresa", product.getCompany() == null ? null : product.getCompany().getName()),
                field("Instruções", product.getInstructions()), field("Dosagem", product.getDosage())));
    }

    @Transactional
    public void removeDisposal(Disposal disposal) {
        remove(disposal, DISPOSAL_PARTS, List.of(
                field("Produto", disposal.getProduct() == null ? null : disposal.getProduct().getName()),
                field("Descarte do produto", disposal.getProductDisposalMethod()),
                field("Descarte da embalagem", disposal.getPackagingDisposalMethod()),
                field("Restrições", disposal.getDisposalRestrictions()),
                field("Precauções ambientais", disposal.getEnvironmentalPrecautions())));
    }

    @Transactional
    public void removeHistoryType(HistoryType historyType) {
        remove(historyType, HISTORY_TYPE_PARTS, List.of(field("Nome", historyType.getName())));
    }

    /** Quem exclui um administrador não pode ser ele mesmo; o log das ações dele é apagado (e vai na cópia). */
    @Transactional
    public void removeAdmin(Admin admin) {
        remove(admin, ADMIN_PARTS, List.of(
                field("Nome", admin.getUserName()), field("E-mail", admin.getEmail()), field("Status", admin.getStatus()),
                field("Cadastro", admin.getSignUpDate())));
    }

    private void remove(Model record, List<Part> parts, List<String> details) {
        Admin author = currentAdmin.require();
        if (record instanceof Admin && record.getId().equals(author.getId())) {
            throw new InvalidDataException("Você não pode excluir a sua própria conta.");
        }
        Integer id = record.getId();
        String table = parts.get(0).table();

        String copy = copyOfRows(table, id, parts);

        List<String> removed = new ArrayList<>();
        for (int i = parts.size() - 1; i >= 0; i--) {
            Part part = parts.get(i);
            int count = entityManager.createNativeQuery("delete from " + part.table() + " where " + part.where())
                    .setParameter("id", id).executeUpdate();
            if (i > 0 && count > 0) {
                removed.add(count + " " + part.label());
            }
        }
        entityManager.detach(record);

        logs.save(AdminLog.builder()
                .admin(author)
                .affectedTable(table)
                .recordId(id)
                .action(AdminAction.DELETE)
                .previousData(RemovalLogFormat.compose(summary(details, removed), copy))
                .build());
    }

    /** JSON com as linhas de cada tabela, na ordem do plano (a ordem em que podem ser reinseridas). */
    private String copyOfRows(String table, Integer id, List<Part> parts) {
        ObjectNode copy = JSON.createObjectNode();
        copy.put("version", 1);
        copy.put("table", table);
        copy.put("id", id);
        ArrayNode tables = copy.putArray("parts");
        for (Part part : parts) {
            String rows = (String) entityManager.createNativeQuery(COPY_ROWS.formatted(part.table(), part.where()))
                    .setParameter("id", id).getSingleResult();
            ObjectNode entry = tables.addObject();
            entry.put("table", part.table());
            entry.set("rows", JSON.readTree(rows));
        }
        return copy.toString();
    }

    /** Dados do registro no momento da exclusão. A senha nunca vai para o texto que aparece na tela. */
    private String summary(List<String> details, List<String> removed) {
        List<String> lines = new ArrayList<>(details);
        lines.add("Apagados junto: " + (removed.isEmpty() ? "nada" : String.join(", ", removed)));
        return String.join("\n", lines);
    }

    private static String field(String label, Object value) {
        return label + ": " + (value == null || value.toString().isBlank() ? "-" : value);
    }
}
