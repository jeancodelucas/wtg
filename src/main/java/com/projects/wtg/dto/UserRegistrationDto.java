package com.projects.wtg.dto;

import com.projects.wtg.model.Account;
import com.projects.wtg.model.User;
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

    public User toUser() {
        User user = new User();
        user.setFullName(this.fullName);
        user.setBirthday(this.birthday);
        user.setPhone(this.phone);
        user.setToken(this.token);
        user.setFirstName(this.firstName);
        return user;
    }

    public Account toAccount() {
        Account account = new Account();
        account.setUserName(this.userName);
        account.setEmail(this.email);
        account.setPassword(this.password);
        account.setConfirmPassword(this.confirmPassword);
        return account;
    }
}
