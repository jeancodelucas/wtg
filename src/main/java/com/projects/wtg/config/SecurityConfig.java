package com.projects.wtg.config;

import com.projects.wtg.service.CustomOidcUserService;
import com.projects.wtg.service.JpaUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomOidcUserService customOidcUserService;
    private final JpaUserDetailsService jpaUserDetailsService;

    public SecurityConfig(CustomOidcUserService customOidcUserService, JpaUserDetailsService jpaUserDetailsService) {
        this.customOidcUserService = customOidcUserService;
        this.jpaUserDetailsService = jpaUserDetailsService;
        logger.info("### SecurityConfig INICIALIZADA com os serviços OIDC e JPA ###");
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .userDetailsService(jpaUserDetailsService) // Define o provedor de usuários para autenticação local

                // Configuração das regras de autorização
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/register", "/api/auth/login", "/error").permitAll()
                        .anyRequest().authenticated()
                )

                // Configuração do Login via SSO (OIDC)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(this.customOidcUserService)
                        )
                        .defaultSuccessUrl("/api/secured/user-info", true)
                )

                // NOVO: Tratamento de exceção para APIs REST
                // Isso instrui o Spring a retornar um erro 401 em vez de redirecionar para uma página HTML
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Não autorizado")
                        )
                );

        return http.build();
    }
}