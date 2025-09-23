package com.projects.wtg.controller;

import com.projects.wtg.dto.UserDto;
import com.projects.wtg.dto.UserRegistrationDto;
import com.projects.wtg.model.User;
import com.projects.wtg.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto, Authentication authentication) {
        User createdUser = userService.createUserWithAccount(registrationDto, authentication);
        UserDto userDto = new UserDto(createdUser);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    // Os endpoints de desativar e deletar foram movidos para UserProfileController
    // para maior segurança e melhor design da API.
}