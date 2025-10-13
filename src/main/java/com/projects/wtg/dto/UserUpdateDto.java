package com.projects.wtg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UserUpdateDto {
    @NotBlank(message = "O campo 'Como quer ser chamado?' não pode estar em branco")
    private String firstName;

    @NotBlank(message = "O CPF não pode estar em branco")
    private String cpf;

    private LocalDate birthday;
    private String pronouns;
}