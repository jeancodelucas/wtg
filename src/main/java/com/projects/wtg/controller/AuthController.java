package com.projects.wtg.controller;

import com.projects.wtg.dto.ForgotPasswordRequestDto;
import com.projects.wtg.dto.LoginRequestDto;
import com.projects.wtg.dto.LoginResponseDto;
import com.projects.wtg.dto.ResetPasswordRequestDto;
import com.projects.wtg.dto.UserDto;
import com.projects.wtg.model.Account;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, AccountRepository accountRepository, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequest, HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
            );

            Authentication authentication = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            Account account = accountRepository.findByEmailWithUserAndPlans(loginRequest.getEmail())
                    .orElseThrow(() -> new IllegalStateException("Usuário logado não encontrado no banco de dados."));

            String message;

            if (!account.getActive()) {
                userService.reactivateAccount(account);
                message = "Usuário foi reativado com sucesso.";
            } else {
                message = "logado";
            }

            UserDto userDto = new UserDto(account.getUser());
            LoginResponseDto response = new LoginResponseDto("ok", message, 200, userDto);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            // Requisito 1: Retorna erro 401 para credenciais inválidas
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "E-mail ou senha inválidos."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDto forgotPasswordRequestDto) {
        userService.generatePasswordResetToken(forgotPasswordRequestDto.getEmail());
        return ResponseEntity.ok(Map.of("message", "Se o e-mail existir em nossa base, um link para redefinição de senha será enviado."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto resetPasswordRequestDto) {
        userService.resetPassword(resetPasswordRequestDto.getToken(), resetPasswordRequestDto.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }
}
