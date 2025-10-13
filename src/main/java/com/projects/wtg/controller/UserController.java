package com.projects.wtg.controller;

import com.projects.wtg.dto.*;
import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import com.projects.wtg.service.PromotionService;
import com.projects.wtg.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final PromotionService promotionService;

    @Autowired
    public UserController(UserService userService, PromotionService promotionService) {
        this.userService = userService;
        this.promotionService = promotionService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDto> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto, Authentication authentication) {
        User createdUser = userService.createUserWithAccount(registrationDto, authentication);
        UserDto userDto = new UserDto(createdUser);

        // Lógica para buscar promoções próximas
        List<PromotionDto> nearbyPromotions = null;
        if (registrationDto.getLatitude() != null && registrationDto.getLongitude() != null) {
            List<Promotion> promotions = promotionService.findWithFilters(null, registrationDto.getLatitude(), registrationDto.getLongitude(), 5.0);
            nearbyPromotions = promotions.stream().map(PromotionDto::new).collect(Collectors.toList());
        }

        RegistrationResponseDto response = new RegistrationResponseDto(userDto, nearbyPromotions);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @PutMapping("/update")
    public ResponseEntity<UserDto> updateUser(@Valid @RequestBody UserUpdateDto userUpdateDto, Authentication authentication) {
        String userEmail = authentication.getName();
        User updatedUser = userService.updateUser(userEmail, userUpdateDto);
        return ResponseEntity.ok(new UserDto(updatedUser));
    }
}
