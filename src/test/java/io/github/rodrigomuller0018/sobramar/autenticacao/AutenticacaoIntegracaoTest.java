package io.github.rodrigomuller0018.sobramar.autenticacao;

import static io.github.rodrigomuller0018.sobramar.ClienteDeTeste.comCsrf;
import static io.github.rodrigomuller0018.sobramar.ClienteDeTeste.cookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rodrigomuller0018.sobramar.ClienteDeTeste;
import io.github.rodrigomuller0018.sobramar.ClienteDeTeste.Credenciais;
import io.github.rodrigomuller0018.sobramar.TestcontainersConfiguration;
import io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao.AutenticacaoService;
import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.Totp;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AutenticacaoIntegracaoTest {

    private static final String BASE = "/api/v1/autenticacao";
    private static final String ACESSO = "sobramar_acesso";
    private static final String RENOVACAO = "sobramar_renovacao";
    private static final String DESAFIO = "sobramar_desafio";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutenticacaoService servico;

    @Test
    void deveAbrirSessaoComCookiesProtegidos() throws Exception {
        Credenciais credenciais = ClienteDeTeste.novaConta(servico);

        MvcResult resultado = ClienteDeTeste.entrar(mvc, credenciais);

        Cookie acesso = cookie(resultado, ACESSO);
        assertThat(acesso.isHttpOnly()).isTrue();
        assertThat(acesso.getSecure()).isTrue();
        assertThat(acesso.getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(cookie(resultado, RENOVACAO).isHttpOnly()).isTrue();

        mvc.perform(get(BASE + "/eu").cookie(acesso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(credenciais.email()))
                .andExpect(jsonPath("$.nomeConta").value("Conta de teste"));
    }

    @Test
    void deveDarOMesmoErroParaSenhaErradaEParaEmailInexistente() throws Exception {
        Credenciais credenciais = ClienteDeTeste.novaConta(servico);

        tentarEntrar(credenciais.email(), "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
        tentarEntrar("ninguem-" + credenciais.email(), "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void deveBloquearDepoisDeMuitasTentativasMesmoComSenhaCerta() throws Exception {
        Credenciais credenciais = ClienteDeTeste.novaConta(servico);
        for (int i = 0; i < 5; i++) {
            tentarEntrar(credenciais.email(), "senha-errada").andExpect(status().isUnauthorized());
        }

        tentarEntrar(credenciais.email(), credenciais.senha())
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.codigo").value("MUITAS_TENTATIVAS"));
    }

    @Test
    void deveRecusarRequisicaoSemSessaoOuComTokenAdulterado() throws Exception {
        mvc.perform(get(BASE + "/eu"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        mvc.perform(get(BASE + "/eu").cookie(new Cookie(ACESSO, "token.adulterado.invalido")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarEscritaSemTokenCsrf() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);

        mvc.perform(post("/api/v1/pessoas")
                        .cookie(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apelido\":\"Ana\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRotacionarARenovacaoEDerrubarTudoSeUmTokenAntigoVoltar() throws Exception {
        MvcResult entrada = ClienteDeTeste.entrar(mvc, ClienteDeTeste.novaConta(servico));
        Cookie primeira = cookie(entrada, RENOVACAO);

        MvcResult renovada = mvc.perform(
                        post(BASE + "/renovar").cookie(primeira).with(comCsrf()))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie segunda = cookie(renovada, RENOVACAO);
        assertThat(segunda.getValue()).isNotEqualTo(primeira.getValue());

        mvc.perform(post(BASE + "/renovar").cookie(primeira).with(comCsrf())).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/renovar").cookie(segunda).with(comCsrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void deveEncerrarARenovacaoAoSair() throws Exception {
        MvcResult entrada = ClienteDeTeste.entrar(mvc, ClienteDeTeste.novaConta(servico));
        Cookie renovacao = cookie(entrada, RENOVACAO);

        MvcResult saida = mvc.perform(post(BASE + "/sair")
                        .cookie(cookie(entrada, ACESSO), renovacao)
                        .with(comCsrf()))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(cookie(saida, ACESSO).getMaxAge()).isZero();
        mvc.perform(post(BASE + "/renovar").cookie(renovacao).with(comCsrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void deveExigirSegundoFatorDepoisDeAtivadoERecusarReusoDoCodigo() throws Exception {
        Credenciais credenciais = ClienteDeTeste.novaConta(servico);
        Cookie acesso = cookie(ClienteDeTeste.entrar(mvc, credenciais), ACESSO);
        String segredo = ativarSegundoFator(acesso);

        MvcResult entrada = ClienteDeTeste.entrar(mvc, credenciais);
        assertThat(entrada.getResponse().getContentAsString()).contains("\"segundoFatorObrigatorio\":true");
        assertThat(cookie(entrada, ACESSO)).isNull();
        Cookie desafio = cookie(entrada, DESAFIO);

        enviarCodigo(desafio, "000000x").andExpect(status().isBadRequest());
        String codigoSeguinte = Totp.codigo(segredo, Instant.now().plusSeconds(30));
        MvcResult confirmado = enviarCodigo(desafio, codigoSeguinte)
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(cookie(confirmado, ACESSO)).isNotNull();

        enviarCodigo(desafio, codigoSeguinte)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void deveRecusarSegundoFatorSemDesafioOuComTokenDeAcesso() throws Exception {
        Cookie acesso = cookie(ClienteDeTeste.entrar(mvc, ClienteDeTeste.novaConta(servico)), ACESSO);

        mvc.perform(post(BASE + "/segundo-fator")
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
        enviarCodigo(new Cookie(DESAFIO, acesso.getValue()), "123456").andExpect(status().isUnauthorized());
    }

    @Test
    void naoDeveAtivarDuasVezesNemConfirmarSemIniciar() throws Exception {
        Cookie acesso = cookie(ClienteDeTeste.entrar(mvc, ClienteDeTeste.novaConta(servico)), ACESSO);

        mvc.perform(post(BASE + "/segundo-fator/ativacao/confirmar")
                        .cookie(acesso)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"123456\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SEGUNDO_FATOR_NAO_INICIADO"));

        ativarSegundoFator(acesso);

        mvc.perform(post(BASE + "/segundo-fator/ativacao").cookie(acesso).with(comCsrf()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SEGUNDO_FATOR_JA_ATIVO"));
    }

    private String ativarSegundoFator(Cookie acesso) throws Exception {
        String corpo = mvc.perform(
                        post(BASE + "/segundo-fator/ativacao").cookie(acesso).with(comCsrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String uri = corpo.substring(corpo.indexOf("otpauth"), corpo.lastIndexOf('"'));
        String segredo =
                UriComponentsBuilder.fromUriString(uri).build().getQueryParams().getFirst("secret");

        mvc.perform(post(BASE + "/segundo-fator/ativacao/confirmar")
                        .cookie(acesso)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"%s\"}".formatted(Totp.codigo(segredo, Instant.now()))))
                .andExpect(status().isNoContent());
        return segredo;
    }

    private org.springframework.test.web.servlet.ResultActions enviarCodigo(Cookie desafio, String codigo)
            throws Exception {
        return mvc.perform(post(BASE + "/segundo-fator")
                .cookie(desafio)
                .with(comCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"%s\"}".formatted(codigo)));
    }

    private org.springframework.test.web.servlet.ResultActions tentarEntrar(String email, String senha)
            throws Exception {
        return mvc.perform(post(BASE + "/entrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }
}
