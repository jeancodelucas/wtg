package com.projects.wtg.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePromotionRequestDto {
    @NotBlank(message = "O campo 'title' não pode estar em branco")
    private String title;

    @NotBlank(message = "O campo 'description' não pode estar em branco")
    private String description;

    @NotNull(message = "O campo 'active' é obrigatório")
    private Boolean active;

    private boolean free;
    private String obs;

    @Valid // Valida o objeto aninhado
    @NotNull(message = "O objeto 'address' é obrigatório")
    private AddressDto address;
}