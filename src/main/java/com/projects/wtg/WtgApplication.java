package com.projects.wtg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling; // Importe

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling // Habilita o agendador
public class WtgApplication {

    public static void main(String[] args) {
        SpringApplication.run(WtgApplication.class, args);
    }

}