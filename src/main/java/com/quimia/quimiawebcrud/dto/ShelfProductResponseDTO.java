package com.quimia.quimiawebcrud.dto;

import lombok.*;

import java.time.LocalDateTime;

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
