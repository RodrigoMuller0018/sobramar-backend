package io.github.rodrigomuller0018.sobramar;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao.AutenticacaoService;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Cria contas e abre sessões pelo caminho real da API. */
public final class ClienteDeTeste {

    private ClienteDeTeste() {}

    public record Credenciais(String email, String senha) {}

    public static Credenciais novaConta(AutenticacaoService servico) {
        Credenciais credenciais = new Credenciais(
                "pessoa-" + UUID.randomUUID() + "@exemplo.test",
                UUID.randomUUID().toString());
        servico.criarContaSeAusente(credenciais.email(), credenciais.senha(), "Conta de teste");
        return credenciais;
    }

    public static MvcResult entrar(MockMvc mvc, Credenciais credenciais) throws Exception {
        return mvc.perform(post("/api/v1/autenticacao/entrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}"
                                .formatted(credenciais.email(), credenciais.senha())))
                .andExpect(status().isOk())
                .andReturn();
    }

    public static Cookie[] sessao(MockMvc mvc, AutenticacaoService servico) throws Exception {
        return entrar(mvc, novaConta(servico)).getResponse().getCookies();
    }

    public static Cookie cookie(MvcResult resultado, String nome) {
        return resultado.getResponse().getCookie(nome);
    }

    /** Atalho para o token CSRF que o front envia em toda escrita. */
    public static org.springframework.test.web.servlet.request.RequestPostProcessor comCsrf() {
        return csrf();
    }
}
