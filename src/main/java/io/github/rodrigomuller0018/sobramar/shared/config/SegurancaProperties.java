package io.github.rodrigomuller0018.sobramar.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Segredos em Base64, com ao menos 32 bytes. Sem valor padrão: a aplicação não sobe sem eles. */
@Validated
@ConfigurationProperties("sobramar.seguranca")
public record SegurancaProperties(
        @NotBlank String jwtSegredo, @NotBlank String chaveCifragem, boolean cookieSeguro) {}
