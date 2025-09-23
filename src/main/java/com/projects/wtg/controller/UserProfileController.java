package com.projects.wtg.controller;

import com.projects.wtg.dto.UserDto;
import com.projects.wtg.model.User;
import com.projects.wtg.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.projects.wtg.dto.LoginResponseDto;

@RestController
@RequestMapping("/api/profile/me") // Endpoint base para o perfil do usuário logado
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint para desativar a própria conta
    @PatchMapping("/deactivate")
    public ResponseEntity<LoginResponseDto> deactivateOwnAccount(Authentication authentication) {
        String userEmail = authentication.getName();
        User deactivatedUser = userService.deactivateUserByEmail(userEmail);

        UserDto userDto = new UserDto(deactivatedUser);
        // Cria a resposta customizada
        LoginResponseDto response = new LoginResponseDto("ok", "usuário desativado", 200, userDto);

        return ResponseEntity.ok(response);
    }

    // Endpoint para excluir a própria conta
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteOwnAccount(Authentication authentication) {
        String userEmail = authentication.getName();
        userService.deleteUserByEmail(userEmail);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user-info")
    public ResponseEntity<UserDto> getUserInfo(Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userService.findUserByEmail(userEmail);
        return ResponseEntity.ok(new UserDto(user));
    }
}