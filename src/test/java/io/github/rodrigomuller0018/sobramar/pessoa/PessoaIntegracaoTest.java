package io.github.rodrigomuller0018.sobramar.pessoa;

import static io.github.rodrigomuller0018.sobramar.ClienteDeTeste.comCsrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rodrigomuller0018.sobramar.ClienteDeTeste;
import io.github.rodrigomuller0018.sobramar.TestcontainersConfiguration;
import io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao.AutenticacaoService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PessoaIntegracaoTest {

    private static final String BASE = "/api/v1/pessoas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutenticacaoService servico;

    @Test
    void deveCriarRenomearEArquivarSemApagarOHistorico() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);

        String local = criar(sessao, "  Ana  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apelido").value("Ana"))
                .andExpect(jsonPath("$.arquivada").value(false))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(put(local)
                        .cookie(sessao)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apelido\":\"Aninha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apelido").value("Aninha"));

        mvc.perform(delete(local).cookie(sessao).with(comCsrf())).andExpect(status().isNoContent());

        mvc.perform(get(BASE).cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get(BASE).cookie(sessao).param("arquivadas", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].apelido").value("Aninha"))
                .andExpect(jsonPath("$.content[0].arquivada").value(true));
    }

    @Test
    void deveRecusarApelidoDuplicadoSemDiferenciarMaiusculas() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);
        criar(sessao, "Ana").andExpect(status().isCreated());

        criar(sessao, "ANA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("APELIDO_DUPLICADO"));
    }

    @Test
    void deveValidarOApelido() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);

        criar(sessao, "   ").andExpect(status().isBadRequest());
        criar(sessao, "a".repeat(41)).andExpect(status().isBadRequest());
        criar(sessao, "a".repeat(40)).andExpect(status().isCreated());
    }

    @Test
    void deveLimitarOTamanhoDaPagina() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);

        mvc.perform(get(BASE).cookie(sessao).param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
    }

    private ResultActions criar(Cookie[] sessao, String apelido) throws Exception {
        return mvc.perform(post(BASE)
                .cookie(sessao)
                .with(comCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"apelido\":\"%s\"}".formatted(apelido)));
    }
}
