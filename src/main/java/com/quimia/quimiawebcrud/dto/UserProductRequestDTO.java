package com.quimia.quimiawebcrud.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProductRequestDTO {

    private Integer userId;
    private Integer productId;
}
