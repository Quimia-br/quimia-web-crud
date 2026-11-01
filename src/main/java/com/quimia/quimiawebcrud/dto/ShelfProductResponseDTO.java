package com.quimia.quimiawebcrud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Dados de produto de uma estante que as telas mostram.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShelfProductResponseDTO {

    private Integer shelfId;
    private Integer productId;
    private LocalDateTime addedAt;
}
