package io.github.rodrigomuller0018.sobramar.autenticacao.api;

import io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao.AutenticacaoService;
import io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao.AutenticacaoService.ResultadoEntrada;
import io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao.AutenticacaoService.TokensDeSessao;
import io.github.rodrigomuller0018.sobramar.shared.erro.CredenciaisInvalidasException;
import io.github.rodrigomuller0018.sobramar.shared.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autenticacao")
class AutenticacaoController {

    record EntrarRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 200) String senha) {}

    record EntrarResponse(boolean segundoFatorObrigatorio) {}

    record CodigoRequest(
            @NotBlank @Pattern(regexp = "\\d{6}") String codigo) {}

    record AtivacaoResponse(String uriOtpauth) {}

    private final AutenticacaoService servico;
    private final CookiesDeSessao cookies;

    AutenticacaoController(AutenticacaoService servico, CookiesDeSessao cookies) {
        this.servico = servico;
        this.cookies = cookies;
    }

    @PostMapping("/entrar")
    ResponseEntity<EntrarResponse> entrar(@Valid @RequestBody EntrarRequest requisicao) {
        ResultadoEntrada resultado = servico.entrar(requisicao.email(), requisicao.senha());
        ResponseEntity.BodyBuilder resposta = ResponseEntity.ok();
        if (resultado.segundoFatorObrigatorio()) {
            resposta.header(
                    HttpHeaders.SET_COOKIE,
                    cookies.desafio(resultado.tokenDesafio()).toString());
        } else {
            definirSessao(resposta, resultado.sessao());
        }
        return resposta.body(new EntrarResponse(resultado.segundoFatorObrigatorio()));
    }

    @PostMapping("/segundo-fator")
    ResponseEntity<Void> segundoFator(
            @CookieValue(name = CookiesDeSessao.DESAFIO, required = false) String desafio,
            @Valid @RequestBody CodigoRequest requisicao) {
        if (desafio == null) {
            throw new CredenciaisInvalidasException();
        }
        ResponseEntity.BodyBuilder resposta = ResponseEntity.status(204)
                .header(HttpHeaders.SET_COOKIE, cookies.apagarDesafio().toString());
        definirSessao(resposta, servico.confirmarSegundoFator(desafio, requisicao.codigo()));
        return resposta.build();
    }

    @PostMapping("/renovar")
    ResponseEntity<Void> renovar(@CookieValue(name = CookiesDeSessao.RENOVACAO, required = false) String renovacao) {
        if (renovacao == null) {
            throw new CredenciaisInvalidasException();
        }
        ResponseEntity.BodyBuilder resposta = ResponseEntity.status(204);
        definirSessao(resposta, servico.renovar(renovacao));
        return resposta.build();
    }

    @PostMapping("/sair")
    ResponseEntity<Void> sair(@CookieValue(name = CookiesDeSessao.RENOVACAO, required = false) String renovacao) {
        servico.sair(renovacao);
        return ResponseEntity.status(204)
                .header(HttpHeaders.SET_COOKIE, cookies.apagarAcesso().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.apagarRenovacao().toString())
                .build();
    }

    @GetMapping("/eu")
    AutenticacaoService.DadosDoUsuario eu(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.dadosDoUsuario(usuario);
    }

    @PostMapping("/segundo-fator/ativacao")
    AtivacaoResponse iniciarAtivacao(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return new AtivacaoResponse(servico.iniciarAtivacaoDoSegundoFator(usuario.usuarioId()));
    }

    @PostMapping("/segundo-fator/ativacao/confirmar")
    ResponseEntity<Void> confirmarAtivacao(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody CodigoRequest requisicao) {
        servico.confirmarAtivacaoDoSegundoFator(usuario.usuarioId(), requisicao.codigo());
        return ResponseEntity.noContent().build();
    }

    private void definirSessao(ResponseEntity.BodyBuilder resposta, TokensDeSessao sessao) {
        resposta.header(
                HttpHeaders.SET_COOKIE, cookies.acesso(sessao.tokenAcesso()).toString());
        resposta.header(
                HttpHeaders.SET_COOKIE,
                cookies.renovacao(sessao.tokenRenovacao()).toString());
    }
}
