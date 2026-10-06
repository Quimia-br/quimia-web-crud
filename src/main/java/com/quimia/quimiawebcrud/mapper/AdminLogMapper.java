package com.quimia.quimiawebcrud.mapper;

import com.quimia.quimiawebcrud.dto.AdminLogRequestDTO;
import com.quimia.quimiawebcrud.dto.AdminLogResponseDTO;
import com.quimia.quimiawebcrud.model.AdminLog;
import org.springframework.stereotype.Component;

import static com.quimia.quimiawebcrud.mapper.References.admin;
import static com.quimia.quimiawebcrud.mapper.References.idOf;

@Component
public class AdminLogMapper implements Mapper<AdminLog, AdminLogRequestDTO, AdminLogResponseDTO> {

    @Override
    public AdminLog toEntity(AdminLogRequestDTO request) {
        if (request == null) {
            return null;
        }
        return AdminLog.builder()
                .admin(admin(request.getAdminId()))
                .affectedTable(request.getAffectedTable())
                .recordId(request.getRecordId())
                .action(request.getAction())
                .previousData(request.getPreviousData())
                .editDate(request.getEditDate())
                .build();
    }

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
                .previousData(entity.getPreviousData())
                .editDate(entity.getEditDate())
                .build();
    }

    @Override
    public void updateEntity(AdminLog entity, AdminLogRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setAdmin(admin(request.getAdminId()));
        entity.setAffectedTable(request.getAffectedTable());
        entity.setRecordId(request.getRecordId());
        entity.setAction(request.getAction());
        entity.setPreviousData(request.getPreviousData());
        if (request.getEditDate() != null) {
            entity.setEditDate(request.getEditDate());
        }
    }
}
