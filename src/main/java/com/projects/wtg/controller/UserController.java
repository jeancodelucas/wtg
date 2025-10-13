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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto registrationDto, Authentication authentication) {

        boolean isInitiationStep = registrationDto.getEmail() != null && registrationDto.getFullName() == null && registrationDto.getToken() == null;
        boolean isTokenValidationStep = registrationDto.getEmail() != null && registrationDto.getToken() != null && registrationDto.getFullName() == null;
        boolean isFinalRegistrationStep = registrationDto.getEmail() != null && registrationDto.getFullName() != null;

        // ETAPA 1: Apenas o e-mail é fornecido para iniciar o registo.
        if (isInitiationStep) {
            userService.initiateRegistration(registrationDto.getEmail());
            return ResponseEntity.ok(Map.of("message", "Token gerado e enviado para o seu e-mail."));
        }

        // ETAPA 2: E-mail e token são fornecidos para validação.
        if (isTokenValidationStep) {
            userService.validateRegistrationToken(registrationDto.getEmail(), registrationDto.getToken());
            return ResponseEntity.ok(Map.of("message", "Token validado com sucesso."));
        }

        // ETAPA 3: Todos os dados são fornecidos para completar o registo.
        // A validação completa só acontece nesta etapa.
        if (isFinalRegistrationStep) {
            // Aqui podemos chamar um método que aplica a validação @Valid programaticamente se necessário,
            // mas as validações dentro do `createUserWithAccount` já tratam disso.
            User createdUser = userService.createUserWithAccount(registrationDto, authentication);
            UserDto userDto = new UserDto(createdUser);

            List<PromotionDto> nearbyPromotions = null;
            if (registrationDto.getLatitude() != null && registrationDto.getLongitude() != null) {
                // Assumindo um raio padrão de 5km se não for especificado
                List<Promotion> promotions = promotionService.findWithFilters(null, registrationDto.getLatitude(), registrationDto.getLongitude(), 5.0);
                nearbyPromotions = promotions.stream().map(PromotionDto::new).collect(Collectors.toList());
            }

            RegistrationResponseDto response = new RegistrationResponseDto(userDto, nearbyPromotions);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

        // Se nenhuma das condições for atendida, a requisição é inválida.
        return ResponseEntity.badRequest().body(Map.of("error", "Requisição de registo inválida. Forneça os campos corretos para a etapa desejada."));
    }

    @PutMapping("/update")
    public ResponseEntity<UserDto> updateUser(@Valid @RequestBody UserUpdateDto userUpdateDto, Authentication authentication) {
        String userEmail = authentication.getName();
        User updatedUser = userService.updateUser(userEmail, userUpdateDto);
        return ResponseEntity.ok(new UserDto(updatedUser));
    }
}