package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.response.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.mapper.support.ResponseMapper;
import com.quimia.quimiawebcrud.model.AdminLog;
import com.quimia.quimiawebcrud.service.removal.RemovalLogFormat;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.support.References.idOf;

/**
 * Converte registro do log de edições entre entidade e DTO.
 */
@Component
public class AdminLogMapper implements ResponseMapper<AdminLog, AdminLogResponseDTO> {

    @Override
    public AdminLogResponseDTO toResponse(AdminLog entity) {
        if (entity == null) {
            return null;
        }
        return AdminLogResponseDTO.builder()
                .id(entity.getId())
                .adminId(idOf(entity.getAdmin()))
                .affectedTable(entity.getAffectedTable())
                .recordId(entity.getRecordId())
                .action(entity.getAction())
                .previousData(RemovalLogFormat.summaryOf(entity.getPreviousData()))
                .restore(RemovalLogFormat.isRestore(entity.getAction(), entity.getPreviousData()))
                .editDate(entity.getEditDate())
                .build();
    }
}
