package com.quimia.quimiawebcrud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dados de histórico que as telas mostram.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HistoryResponseDTO extends BaseResponseDTO {

    private Integer shelfId;
    private Integer userId;
    private Integer historyTypeId;
    private LocalDateTime executionDate;
    private String resultDescription;
    private List<String> productNames;
}
