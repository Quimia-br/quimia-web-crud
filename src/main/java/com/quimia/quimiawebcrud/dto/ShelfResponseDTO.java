package com.quimia.quimiawebcrud.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShelfResponseDTO extends BaseResponseDTO {

    private Integer userId;
    private String room;
}
