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

@Component // Marca esta classe como um bean do Spring para que possamos injetá-la
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(); // Para converter objetos em JSON

    public CustomAuthenticationSuccessHandler(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 1. Obter o usuário autenticado via SSO
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();

        // 2. Buscar a conta e o usuário no banco de dados
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário SSO logado não encontrado no banco de dados."));

        // 3. Criar os DTOs de resposta
        UserDto userDto = new UserDto(account.getUser());
        LoginResponseDto loginResponse = new LoginResponseDto("ok", "logado com SSO", 200, userDto);

        // 4. Configurar a resposta HTTP
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 5. Escrever o objeto JSON diretamente na resposta
        response.getWriter().write(objectMapper.writeValueAsString(loginResponse));
        response.getWriter().flush();
    }
}