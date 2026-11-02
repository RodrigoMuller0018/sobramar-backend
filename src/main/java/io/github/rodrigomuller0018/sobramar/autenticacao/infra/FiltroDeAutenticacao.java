package io.github.rodrigomuller0018.sobramar.autenticacao.infra;

import io.github.rodrigomuller0018.sobramar.autenticacao.api.CookiesDeSessao;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

/** Lê o access token do cookie HttpOnly. Token ausente ou inválido segue anônimo, e a regra de acesso decide. */
@Component
public class FiltroDeAutenticacao extends OncePerRequestFilter {

    private final ServicoDeTokens tokens;

    public FiltroDeAutenticacao(ServicoDeTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        var cookie = WebUtils.getCookie(requisicao, CookiesDeSessao.ACESSO);
        if (cookie != null) {
            tokens.lerAcesso(cookie.getValue())
                    .ifPresent(usuario -> SecurityContextHolder.getContext()
                            .setAuthentication(
                                    UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of())));
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
