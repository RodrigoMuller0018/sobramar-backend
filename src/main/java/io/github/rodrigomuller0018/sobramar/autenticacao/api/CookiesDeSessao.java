package io.github.rodrigomuller0018.sobramar.autenticacao.api;

import io.github.rodrigomuller0018.sobramar.shared.config.SegurancaProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Tokens só em cookie HttpOnly e SameSite=Strict: JavaScript não os lê e outros sites não os enviam. */
@Component
public class CookiesDeSessao {

    public static final String ACESSO = "sobramar_acesso";
    public static final String RENOVACAO = "sobramar_renovacao";
    public static final String DESAFIO = "sobramar_desafio";

    private static final String CAMINHO_API = "/api";
    private static final String CAMINHO_AUTENTICACAO = "/api/v1/autenticacao";

    private final boolean seguro;

    CookiesDeSessao(SegurancaProperties propriedades) {
        this.seguro = propriedades.cookieSeguro();
    }

    ResponseCookie acesso(String token) {
        return cookie(ACESSO, token, CAMINHO_API, Duration.ofMinutes(15));
    }

    ResponseCookie renovacao(String token) {
        return cookie(RENOVACAO, token, CAMINHO_AUTENTICACAO, Duration.ofDays(14));
    }

    ResponseCookie desafio(String token) {
        return cookie(DESAFIO, token, CAMINHO_AUTENTICACAO, Duration.ofMinutes(5));
    }

    ResponseCookie apagarAcesso() {
        return cookie(ACESSO, "", CAMINHO_API, Duration.ZERO);
    }

    ResponseCookie apagarRenovacao() {
        return cookie(RENOVACAO, "", CAMINHO_AUTENTICACAO, Duration.ZERO);
    }

    ResponseCookie apagarDesafio() {
        return cookie(DESAFIO, "", CAMINHO_AUTENTICACAO, Duration.ZERO);
    }

    private ResponseCookie cookie(String nome, String valor, String caminho, Duration duracao) {
        return ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(seguro)
                .sameSite("Strict")
                .path(caminho)
                .maxAge(duracao)
                .build();
    }
}
