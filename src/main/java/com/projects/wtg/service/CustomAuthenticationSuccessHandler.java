package com.projects.wtg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projects.wtg.dto.LoginResponseDto;
import com.projects.wtg.dto.UserDto;
import com.projects.wtg.model.Account;
import com.projects.wtg.repository.AccountRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AccountRepository accountRepository;
    private final UserService userService; // 1. Injete o UserService
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 2. Adicione o UserService ao construtor
    public CustomAuthenticationSuccessHandler(AccountRepository accountRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();

        // --- LÓGICA DE REATIVAÇÃO UNIFICADA ---
        Account account = accountRepository.findByEmailWithUserAndPlans(email)
                .orElseThrow(() -> new IllegalStateException("Usuário SSO logado não encontrado no banco de dados."));

        String message;

        // 3. Verifica se a conta estava inativa
        if (!account.getActive()) {
            // 4. Reativa a conta através do serviço
            userService.reactivateAccount(account);
            message = "Usuário foi reativado com sucesso.";
        } else {
            message = "logado com SSO";
        }

        UserDto userDto = new UserDto(account.getUser());
        LoginResponseDto loginResponse = new LoginResponseDto("ok", message, 200, userDto);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(objectMapper.writeValueAsString(loginResponse));
        response.getWriter().flush();
    }
}