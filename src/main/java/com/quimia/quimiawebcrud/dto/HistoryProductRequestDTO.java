package com.quimia.quimiawebcrud.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryProductRequestDTO {

    private Integer historyId;
    private Integer userId;
    private Integer productId;
}
