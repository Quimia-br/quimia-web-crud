package com.quimia.quimiawebcrud.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Dados de produto de um histórico que as telas mostram.
 */
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
