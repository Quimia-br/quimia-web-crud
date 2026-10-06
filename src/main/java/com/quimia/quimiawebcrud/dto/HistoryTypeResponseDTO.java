package com.quimia.quimiawebcrud.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HistoryTypeResponseDTO extends BaseResponseDTO {

    private String name;
}
