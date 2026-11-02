package io.github.rodrigomuller0018.sobramar;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL real para testes de integração (nunca H2: esconde diferenças de dialeto). */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
    }

    /** Chaves geradas a cada execução: nenhum segredo, nem de teste, fica no Git. */
    @Bean
    DynamicPropertyRegistrar segredosDeTeste() {
        return registro -> {
            registro.add("sobramar.seguranca.jwt-segredo", TestcontainersConfiguration::chaveAleatoria);
            registro.add("sobramar.seguranca.chave-cifragem", TestcontainersConfiguration::chaveAleatoria);
            registro.add("sobramar.seguranca.cookie-seguro", () -> "true");
        };
    }

    private static String chaveAleatoria() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
