package com.projects.wtg.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserRegistrationDto {
    @NotBlank(message = "O nome completo não pode estar em branco")
    private String fullName;

    private LocalDate birthday;
    private String phone;
    private String token;

    @NotBlank(message = "O primeiro nome não pode estar em branco")
    private String firstName;

    private String pictureUrl;

    @NotBlank(message = "O nome de usuário não pode estar em branco")
    private String userName;

    @Email(message = "O formato do e-mail é inválido")
    @NotBlank(message = "O e-mail não pode estar em branco")
    private String email;

    private String secondEmail;

    @NotBlank(message = "A senha não pode estar em branco")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
    // A validação de padrão forte (regex) será mantida no serviço por complexidade
    private String password;

    @NotBlank(message = "A confirmação de senha não pode estar em branco")
    private String confirmPassword;

    private Long planId;
    private boolean emailVerified;
    private String locale;
    private String loginSub;
    private String loginProvider;
    private Double latitude;
    private Double longitude;


    @Valid
    private PromotionDataDto promotion;
}