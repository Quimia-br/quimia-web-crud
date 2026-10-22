package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.AdminAction;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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
