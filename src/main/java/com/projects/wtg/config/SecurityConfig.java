package com.projects.wtg.config;

import com.projects.wtg.service.CustomOidcUserService;
import com.projects.wtg.service.JpaUserDetailsService;
import com.projects.wtg.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.projects.wtg.service.CustomAuthenticationSuccessHandler;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomOidcUserService customOidcUserService;
    private final JpaUserDetailsService jpaUserDetailsService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final UserService userService;

    public SecurityConfig(CustomOidcUserService customOidcUserService, JpaUserDetailsService jpaUserDetailsService, CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler, UserService userService) {
        this.customOidcUserService = customOidcUserService;
        this.jpaUserDetailsService = jpaUserDetailsService;
        this.userService = userService;
        logger.info("### SecurityConfig INICIALIZADA com os serviços OIDC e JPA ###");
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
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
                .cors(withDefaults())
                .userDetailsService(jpaUserDetailsService)
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers(
                                "/api/users/register",
                                "/api/auth/login",
                                "/api/auth/google",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/geocoding/from-address",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/api/comments/**").authenticated()
                        // Protege todos os endpoints de perfil do usuário logado
                        .requestMatchers("/api/profile/me/**").authenticated()
                        .requestMatchers("/api/promotions/**").authenticated() // Protege os endpoints de promoção
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(this.customOidcUserService)
                        )
                        .successHandler(customAuthenticationSuccessHandler)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Não autorizado")
                        )
                );

        return http.build();
    }
}