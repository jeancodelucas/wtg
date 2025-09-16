package com.projects.wtg.controller;

import com.projects.wtg.dto.UserRegistrationDto;
import com.projects.wtg.model.User;
import com.projects.wtg.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody UserRegistrationDto registrationDto) {
        try {
            User newUser = userService.createUserWithAccount(registrationDto);
            return new ResponseEntity<>(newUser, HttpStatus.CREATED);
        } catch (Exception e) {
            // Em um projeto real, você lidaria com exceções de forma mais específica
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
}

