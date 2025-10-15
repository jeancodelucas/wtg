// src/main/java/com/projects/wtg/dto/CreateCommentRequestDto.java

package com.projects.wtg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequestDto {

    @NotNull(message = "O ID da promoção não pode ser nulo.")
    private Long promotionId;

    @NotBlank(message = "O comentário não pode estar em branco.")
    @Size(max = 500, message = "O comentário não pode exceder 500 caracteres.")
    private String comment;
}