package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.model.Admin;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.User;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import com.quimia.quimiawebcrud.repository.AdminRepository;
import com.quimia.quimiawebcrud.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Exclusão de um usuário junto com tudo que é dele (o banco não deixa apagar quem ainda tem histórico,
 * estantes ou localização). Os dados do usuário ficam registrados no log de edições, com o administrador
 * que fez a exclusão.
 */
@Service
@RequiredArgsConstructor
public class UserRemovalService {

    static final String USERS_TABLE = "usuarios";

    /** Ordem importa: cada consulta só apaga o que as seguintes não referenciam mais. */
    static final List<Step> STEPS = List.of(
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

    record Step(String label, String query) {
    }

    private final EntityManager entityManager;
    private final UserRepository users;
    private final AdminRepository admins;
    private final AdminLogRepository logs;

    @Transactional
    public void removeWithRecords(User user) {
        Admin admin = currentAdmin();
        Integer id = user.getId();

        List<String> removed = new ArrayList<>();
        for (Step step : STEPS) {
            int count = entityManager.createQuery(step.query()).setParameter("id", id).executeUpdate();
            if (count > 0) {
                removed.add(count + " " + step.label());
            }
        }

        logs.save(AdminLog.builder()
                .admin(admin)
                .affectedTable(USERS_TABLE)
                .recordId(id)
                .action(AdminAction.DELETE)
                .previousData(snapshot(user, removed))
                .build());

        users.delete(user);
        users.flush();
    }

    /** Dados do usuário no momento da exclusão. A senha nunca vai para o log. */
    private String snapshot(User user, List<String> removed) {
        return String.join("\n",
                "Nome: " + user.getUserName(),
                "E-mail: " + user.getEmail(),
                "Status: " + user.getStatus(),
                "Nascimento: " + (user.getBirthDate() != null ? user.getBirthDate() : "-"),
                "Cadastro: " + (user.getSignUpDate() != null ? user.getSignUpDate() : "-"),
                "Foto: " + (user.getPhotoUrl() != null ? user.getPhotoUrl() : "-"),
                "Apagados junto: " + (removed.isEmpty() ? "nada" : String.join(", ", removed)));
    }

    private Admin currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Exclusão de usuário sem administrador autenticado.");
        }
        return admins.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Administrador da sessão não encontrado."));
    }
}
