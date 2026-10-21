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
import com.quimia.quimiawebcrud.repository.AdminRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Exclusão de um registro junto com tudo que depende dele (o banco não deixa apagar quem ainda é referenciado).
 * Cada tipo tem um plano: consultas que apagam os dependentes, na ordem em que as chaves permitem. Os dados do
 * registro ficam no log de edições, com o administrador que fez a exclusão. Tudo acontece em uma transação:
 * se algo falhar, nada é apagado.
 */
@Service
@RequiredArgsConstructor
public class RecordRemovalService {

    record Step(String label, String query) {
    }

    private static final String COMPANY_PRODUCTS = "(select p.id from Product p where p.company.id = :id)";

    static final List<Step> USER_STEPS = List.of(
            new Step("itens de histórico",
                    "delete from HistoryProduct hp where hp.history.id in "
                            + "(select h.id from History h where h.user.id = :id "
                            + "or h.shelf.id in (select s.id from Shelf s where s.user.id = :id)) "
                            + "or hp.userProduct.id.userId = :id"),
            new Step("históricos",
                    "delete from History h where h.user.id = :id "
                            + "or h.shelf.id in (select s.id from Shelf s where s.user.id = :id)"),
            new Step("produtos nas estantes",
                    "delete from ShelfProduct sp where sp.id.shelfId in (select s.id from Shelf s where s.user.id = :id)"),
            new Step("estantes", "delete from Shelf s where s.user.id = :id"),
            new Step("produtos do usuário", "delete from UserProduct up where up.id.userId = :id"),
            new Step("localizações", "delete from Location l where l.user.id = :id"));

    static final List<Step> PRODUCT_STEPS = List.of(
            new Step("itens de histórico", "delete from HistoryProduct hp where hp.userProduct.id.productId = :id"),
            new Step("produtos nas estantes", "delete from ShelfProduct sp where sp.id.productId = :id"),
            new Step("produtos dos usuários", "delete from UserProduct up where up.id.productId = :id"),
            new Step("fichas de descarte", "delete from Disposal d where d.product.id = :id"));

    static final List<Step> COMPANY_STEPS = List.of(
            new Step("itens de histórico",
                    "delete from HistoryProduct hp where hp.userProduct.id.productId in " + COMPANY_PRODUCTS),
            new Step("produtos nas estantes", "delete from ShelfProduct sp where sp.id.productId in " + COMPANY_PRODUCTS),
            new Step("produtos dos usuários", "delete from UserProduct up where up.id.productId in " + COMPANY_PRODUCTS),
            new Step("fichas de descarte", "delete from Disposal d where d.product.id in " + COMPANY_PRODUCTS),
            new Step("produtos", "delete from Product p where p.company.id = :id"));

    static final List<Step> HISTORY_TYPE_STEPS = List.of(
            new Step("itens de histórico",
                    "delete from HistoryProduct hp where hp.history.id in "
                            + "(select h.id from History h where h.historyType.id = :id)"),
            new Step("históricos", "delete from History h where h.historyType.id = :id"));

    static final List<Step> ADMIN_STEPS = List.of(
            new Step("registros do log", "delete from AdminLog l where l.admin.id = :id"));

    static final List<Step> DISPOSAL_STEPS = List.of();

    /** Todas as consultas, para o teste que as compila contra o modelo. */
    static List<Step> allSteps() {
        return Stream.of(USER_STEPS, PRODUCT_STEPS, COMPANY_STEPS, HISTORY_TYPE_STEPS, ADMIN_STEPS, DISPOSAL_STEPS)
                .flatMap(List::stream).toList();
    }

    private final EntityManager entityManager;
    private final AdminRepository admins;
    private final AdminLogRepository logs;

    @Transactional
    public void removeUser(User user) {
        remove(user, "usuarios", USER_STEPS, List.of(
                field("Nome", user.getUserName()), field("E-mail", user.getEmail()), field("Status", user.getStatus()),
                field("Nascimento", user.getBirthDate()), field("Cadastro", user.getSignUpDate()),
                field("Foto", user.getPhotoUrl())));
    }

    @Transactional
    public void removeCompany(Company company) {
        remove(company, "usuarios_empresas", COMPANY_STEPS, List.of(
                field("Nome", company.getName()), field("CNPJ", company.getCnpj()), field("E-mail", company.getEmail()),
                field("Porte", company.getSize()), field("Status", company.getStatus()),
                field("Cadastro", company.getSignUpDate()), field("Foto", company.getPhotoUrl())));
    }

    @Transactional
    public void removeProduct(Product product) {
        remove(product, "produtos", PRODUCT_STEPS, List.of(
                field("Nome", product.getName()), field("CAS", product.getCasNumber()), field("Marca", product.getBrand()),
                field("Empresa", product.getCompany() == null ? null : product.getCompany().getName()),
                field("Instruções", product.getInstructions()), field("Dosagem", product.getDosage())));
    }

    @Transactional
    public void removeDisposal(Disposal disposal) {
        remove(disposal, "descartes_fds", DISPOSAL_STEPS, List.of(
                field("Produto", disposal.getProduct() == null ? null : disposal.getProduct().getName()),
                field("Descarte do produto", disposal.getProductDisposalMethod()),
                field("Descarte da embalagem", disposal.getPackagingDisposalMethod()),
                field("Restrições", disposal.getDisposalRestrictions()),
                field("Precauções ambientais", disposal.getEnvironmentalPrecautions())));
    }

    @Transactional
    public void removeHistoryType(HistoryType historyType) {
        remove(historyType, "tipos_historicos", HISTORY_TYPE_STEPS, List.of(field("Nome", historyType.getName())));
    }

    /** Quem exclui um administrador não pode ser ele mesmo; o log das ações dele é apagado junto. */
    @Transactional
    public void removeAdmin(Admin admin) {
        remove(admin, "admins", ADMIN_STEPS, List.of(
                field("Nome", admin.getUserName()), field("E-mail", admin.getEmail()), field("Status", admin.getStatus()),
                field("Cadastro", admin.getSignUpDate())));
    }

    private void remove(Model record, String table, List<Step> steps, List<String> details) {
        Admin author = currentAdmin();
        if (record instanceof Admin && record.getId().equals(author.getId())) {
            throw new InvalidDataException("Você não pode excluir a sua própria conta.");
        }
        Integer id = record.getId();

        List<String> removed = new ArrayList<>();
        for (Step step : steps) {
            int count = entityManager.createQuery(step.query()).setParameter("id", id).executeUpdate();
            if (count > 0) {
                removed.add(count + " " + step.label());
            }
        }

        logs.save(AdminLog.builder()
                .admin(author)
                .affectedTable(table)
                .recordId(id)
                .action(AdminAction.DELETE)
                .previousData(snapshot(details, removed))
                .build());

        entityManager.remove(record);
        entityManager.flush();
    }

    /** Dados do registro no momento da exclusão. A senha nunca vai para o log. */
    private String snapshot(List<String> details, List<String> removed) {
        List<String> lines = new ArrayList<>(details);
        lines.add("Apagados junto: " + (removed.isEmpty() ? "nada" : String.join(", ", removed)));
        return String.join("\n", lines);
    }

    private static String field(String label, Object value) {
        return label + ": " + (value == null || value.toString().isBlank() ? "-" : value);
    }

    private Admin currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Exclusão sem administrador autenticado.");
        }
        return admins.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Administrador da sessão não encontrado."));
    }
}
