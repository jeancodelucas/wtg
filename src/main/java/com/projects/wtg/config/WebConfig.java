package com.projects.wtg.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.multipart.support.MultipartFilter;

@Configuration
public class WebConfig {

    /**
     * Mantém a configuração de CORS existente para permitir requisições do frontend.
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * SOLUÇÃO DEFINITIVA: Registra o MultipartFilter do Spring com a maior precedência possível.
     * Isso garante que ele execute ANTES da cadeia de filtros do Spring Security,
     * processando corretamente as requisições 'multipart/form-data' e evitando o erro 415.
     * Esta abordagem resolve o problema de "Content-Type 'null'" de forma limpa.
     */
    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilterRegistrationBean() {
        final MultipartFilter multipartFilter = new MultipartFilter();
        final FilterRegistrationBean<MultipartFilter> registrationBean = new FilterRegistrationBean<>(multipartFilter);

        // Define a ordem para a mais alta prioridade, executando antes de todos os outros filtros.
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registrationBean;
    }
}