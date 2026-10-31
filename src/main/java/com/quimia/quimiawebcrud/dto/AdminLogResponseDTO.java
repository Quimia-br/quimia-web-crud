package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.AdminAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Dados de registro do log de edições que as telas mostram.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminLogResponseDTO extends BaseResponseDTO {

    private Integer adminId;
    private String affectedTable;
    private Integer recordId;
    private AdminAction action;
    private String previousData;
    /** Linha que registra uma restauração (gravada como INSERT no banco). */
    private boolean restore;
    private LocalDateTime editDate;
}
