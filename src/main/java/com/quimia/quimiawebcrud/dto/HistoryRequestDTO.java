package com.quimia.quimiawebcrud.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryRequestDTO {

    private Integer shelfId;
    private Integer userId;
    private Integer historyTypeId;
    private LocalDateTime executionDate;
    private String resultDescription;
}
