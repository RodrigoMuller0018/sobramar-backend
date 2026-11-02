package io.github.rodrigomuller0018.template;

import org.springframework.boot.SpringApplication;

public class TestTemplateBackendSpringApplication {

    public static void main(String[] args) {
        SpringApplication.from(TemplateBackendSpringApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
