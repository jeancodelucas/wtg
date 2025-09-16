package com.projects.wtg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Desabilita CSRF (facilita testes no Postman)
                .csrf(csrf -> csrf.disable())

                // Define as regras de autorização
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/register").permitAll() // endpoint público
                        .anyRequest().authenticated() // o resto exige login
                )

                // Usa autenticação Basic Auth
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
