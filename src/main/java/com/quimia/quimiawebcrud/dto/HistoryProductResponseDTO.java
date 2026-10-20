package com.quimia.quimiawebcrud.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HistoryProductResponseDTO extends BaseResponseDTO {

    private Integer historyId;
    private Integer userId;
    private Integer productId;
}
