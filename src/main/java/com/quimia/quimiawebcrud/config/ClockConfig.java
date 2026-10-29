package com.quimia.quimiawebcrud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * O relógio do sistema é um componente à parte para os testes poderem fixar a data de "hoje".
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
