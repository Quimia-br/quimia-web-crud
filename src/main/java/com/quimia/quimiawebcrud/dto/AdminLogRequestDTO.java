package com.quimia.quimiawebcrud.dto;

import com.quimia.quimiawebcrud.model.enums.AdminAction;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLogRequestDTO {

    private Integer adminId;
    private String affectedTable;
    private Integer recordId;
    private AdminAction action;
    private String previousData;
    private LocalDateTime editDate;
}
