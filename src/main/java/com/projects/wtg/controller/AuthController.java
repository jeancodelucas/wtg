package com.projects.wtg.controller;

import com.projects.wtg.dto.LoginRequestDto;
import com.projects.wtg.dto.UserDto;
import com.projects.wtg.model.Account;
import com.projects.wtg.repository.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.projects.wtg.dto.LoginResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;

    public AuthController(AuthenticationManager authenticationManager, AccountRepository accountRepository) {
        this.authenticationManager = authenticationManager;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequest, HttpServletRequest request) {
        // Cria um token de autenticação com o email e a senha fornecidos
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        // O AuthenticationManager usará nosso JpaUserDetailsService e PasswordEncoder para validar o token
        Authentication authentication = authenticationManager.authenticate(token);

        // Se a autenticação for bem-sucedida, o Spring Security cria um Principal
        // Nós o colocamos no contexto de segurança e criamos a sessão
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = request.getSession(true); // Cria uma nova sessão
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        Account account = accountRepository.findByEmailWithUserAndPlans(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalStateException("Usuário logado não encontrado no banco de dados."));

        UserDto userDto = new UserDto(account.getUser());
        LoginResponseDto response = new LoginResponseDto("ok", "logado", 200, userDto);
        return ResponseEntity.ok(response);
    }
}