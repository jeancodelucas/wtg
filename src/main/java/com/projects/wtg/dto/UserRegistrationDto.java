package com.projects.wtg.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserRegistrationDto {
    private String fullName;
    private LocalDate birthday;
    private String phone;
    private String token;
    private String firstName;
    private String pictureUrl;

    private String userName;
    private String email;
    private String secondEmail;
    private String password;
    private String confirmPassword;
    private boolean emailVerified;
    private String locale;
    private String loginSub;
    private String loginProvider;
}
