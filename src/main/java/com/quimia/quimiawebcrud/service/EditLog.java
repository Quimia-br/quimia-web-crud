package com.quimia.quimiawebcrud.service;

import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.model.BaseEntity;
import com.quimia.quimiawebcrud.model.enums.AdminAction;
import com.quimia.quimiawebcrud.repository.AdminLogRepository;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Registra no log de edições quem criou ou alterou um registro pela tela.
 * Na edição, o "dado anterior" é o resumo do registro antes da alteração (sem a senha).
 */
@Component
@RequiredArgsConstructor
public class EditLog {

    private final CurrentAdmin currentAdmin;
    private final AdminLogRepository logs;

    public void created(BaseEntity record) {
        save(record, AdminAction.INSERT, null);
    }

    public void updated(BaseEntity record, String before) {
        save(record, AdminAction.UPDATE, before);
    }

    private void save(BaseEntity record, AdminAction action, String previousData) {
        logs.save(AdminLog.builder()
                .admin(currentAdmin.require())
                .affectedTable(tableOf(record))
                .recordId(record.getId())
                .action(action)
                .previousData(previousData)
                .build());
    }

    static String tableOf(BaseEntity record) {
        return record.getClass().getAnnotation(Table.class).name();
    }
}
