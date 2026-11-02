package io.github.rodrigomuller0018.template.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio injetável: regras de negócio nunca chamam Instant.now() direto, para os testes controlarem o tempo. */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
