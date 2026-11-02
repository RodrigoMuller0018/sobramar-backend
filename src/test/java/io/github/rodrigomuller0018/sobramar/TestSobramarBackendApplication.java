package io.github.rodrigomuller0018.sobramar;

import org.springframework.boot.SpringApplication;

public class TestSobramarBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(SobramarBackendApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
