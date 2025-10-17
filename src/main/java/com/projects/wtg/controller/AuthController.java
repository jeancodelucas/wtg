// src/main/java/com/projects/wtg/controller/AuthController.java

package com.projects.wtg.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.projects.wtg.dto.*;
import com.projects.wtg.model.Account;
import com.projects.wtg.model.Promotion;
import com.projects.wtg.model.User;
import com.projects.wtg.repository.AccountRepository;
import com.projects.wtg.service.PromotionService;
import com.projects.wtg.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;
    private final UserService userService;
    private final PromotionService promotionService;

    public AuthController(AuthenticationManager authenticationManager, AccountRepository accountRepository, UserService userService, PromotionService promotionService) {
        this.authenticationManager = authenticationManager;
        this.accountRepository = accountRepository;
        this.userService = userService;
        this.promotionService = promotionService;
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleWebClientId;
    @Value("${google.ios.client-id}")
    private String googleIosClientId;


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
            userService.updateUserLocation(loginRequest.getEmail(), loginRequest.getLatitude(), loginRequest.getLongitude());

            Account account = accountRepository.findByEmailWithUserAndPlans(loginRequest.getEmail())
                    .orElseThrow(() -> new IllegalStateException("Usuário logado não encontrado no banco de dados."));

            String message;
            if (!account.getActive()) {
                userService.reactivateAccount(account);
                message = "Usuário foi reativado com sucesso.";
            } else {
                message = "logado";
            }

            User user = account.getUser();
            UserDto userDto = new UserDto(user);
            boolean isRegistrationComplete = user.getCpf() != null && !user.getCpf().isEmpty() && user.getBirthday() != null;

            List<PromotionDto> nearbyPromotions = null;
            if (loginRequest.getLatitude() != null && loginRequest.getLongitude() != null) {
                List<Promotion> promotions = promotionService.findWithFilters(null, loginRequest.getLatitude(), loginRequest.getLongitude(), 5.0);
                nearbyPromotions = promotions.stream().map(PromotionDto::new).collect(Collectors.toList());
            }

            LoginResponseDto response = new LoginResponseDto("ok", message, 200, userDto, nearbyPromotions);
            response.setIsRegistrationComplete(isRegistrationComplete);

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
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

    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleLoginRequestDto loginRequest, HttpServletRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Arrays.asList(googleWebClientId, googleIosClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(loginRequest.getToken());
            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token do Google inválido."));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();

            Account account = userService.processGoogleUser(payload, loginRequest.getLatitude(), loginRequest.getLongitude());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            User user = account.getUser();
            UserDto userDto = new UserDto(user);
            boolean isRegistrationComplete = user.getCpf() != null && !user.getCpf().isEmpty() && user.getBirthday() != null;

            List<PromotionDto> nearbyPromotions = null;
            if (loginRequest.getLatitude() != null && loginRequest.getLongitude() != null) {
                List<Promotion> promotions = promotionService.findWithFilters(null, loginRequest.getLatitude(), loginRequest.getLongitude(), 5.0);
                nearbyPromotions = promotions.stream().map(PromotionDto::new).collect(Collectors.toList());
            }

            LoginResponseDto response = new LoginResponseDto("ok", "logado com SSO", 200, userDto, nearbyPromotions);
            response.setIsRegistrationComplete(isRegistrationComplete);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erro no login com Google: " + e.getMessage()));
        }
    }
}