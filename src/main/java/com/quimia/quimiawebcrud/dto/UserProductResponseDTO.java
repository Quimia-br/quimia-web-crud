package com.quimia.quimiawebcrud.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProductResponseDTO {

    private Integer userId;
    private Integer productId;
}
