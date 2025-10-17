package com.projects.wtg.controller;

import com.projects.wtg.dto.*;
import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import com.projects.wtg.service.PromotionService;
import com.projects.wtg.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final PromotionService promotionService;
    private final AuthenticationManager authenticationManager; // CORREÇÃO: Injetado o AuthenticationManager

    @Autowired
    public UserController(UserService userService, PromotionService promotionService, AuthenticationManager authenticationManager) { // CORREÇÃO: Adicionado ao construtor
        this.userService = userService;
        this.promotionService = promotionService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto registrationDto, HttpServletRequest request, Authentication authentication) { // CORREÇÃO: Adicionado HttpServletRequest

        boolean isInitiationStep = registrationDto.getEmail() != null && registrationDto.getFullName() == null && registrationDto.getToken() == null;
        boolean isTokenValidationStep = registrationDto.getEmail() != null && registrationDto.getToken() != null && registrationDto.getFullName() == null;
        boolean isFinalRegistrationStep = registrationDto.getEmail() != null && registrationDto.getFullName() != null;

        if (isInitiationStep) {
            userService.initiateRegistration(registrationDto.getEmail());
            return ResponseEntity.ok(Map.of("message", "Token gerado e enviado para o seu e-mail."));
        }

        if (isTokenValidationStep) {
            userService.validateRegistrationToken(registrationDto.getEmail(), registrationDto.getToken());
            return ResponseEntity.ok(Map.of("message", "Token validado com sucesso."));
        }

        if (isFinalRegistrationStep) {
            User createdUser = userService.createUserWithAccount(registrationDto, authentication);

            // --- CORREÇÃO: AUTO-LOGIN APÓS CADASTRO ---
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    registrationDto.getEmail(),
                    registrationDto.getPassword()
            );
            Authentication auth = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            // --- FIM DO AUTO-LOGIN ---

            UserDto userDto = new UserDto(createdUser);

            List<PromotionDto> nearbyPromotions = null;
            if (registrationDto.getLatitude() != null && registrationDto.getLongitude() != null) {
                List<Promotion> promotions = promotionService.findWithFilters(null, registrationDto.getLatitude(), registrationDto.getLongitude(), 5.0);
                nearbyPromotions = promotions.stream().map(PromotionDto::new).collect(Collectors.toList());
            }

            RegistrationResponseDto response = new RegistrationResponseDto(userDto, nearbyPromotions);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Requisição de registo inválida. Forneça os campos corretos para a etapa desejada."));
    }

    @PutMapping("/update")
    public ResponseEntity<UserDto> updateUser(@Valid @RequestBody UserUpdateDto userUpdateDto, Authentication authentication) {
        String userEmail = authentication.getName();
        User updatedUser = userService.updateUser(userEmail, userUpdateDto);
        return ResponseEntity.ok(new UserDto(updatedUser));
    }
}