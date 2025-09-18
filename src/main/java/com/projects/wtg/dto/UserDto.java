package com.projects.wtg.dto;

import com.projects.wtg.model.User;
import lombok.Data;

// DTO para representar os dados do usuário na resposta da API
@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String fullName;
    private String phone;
    private String pictureUrl;
    private String email; // Adicionando o email para conveniência

    // Construtor que converte uma entidade User para este DTO
    public UserDto(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.pictureUrl = user.getPictureUrl();
        if (user.getAccount() != null) {
            this.email = user.getAccount().getEmail();
        }
    }
}