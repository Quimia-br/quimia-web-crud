package com.quimia.quimiawebcrud.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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
}
