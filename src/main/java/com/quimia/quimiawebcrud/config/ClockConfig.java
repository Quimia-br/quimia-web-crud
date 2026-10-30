package com.quimia.quimiawebcrud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Registra o relógio do sistema como bean, para ser injetado onde o "hoje" precisa ser testável (hoje, o dashboard);
 * nos testes ele é trocado por um relógio fixo. Usa o fuso horário do servidor, o mesmo do
 * {@code LocalDateTime.now()} que as entidades usam ao gravar as datas.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
