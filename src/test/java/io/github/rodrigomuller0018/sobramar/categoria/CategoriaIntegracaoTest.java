package io.github.rodrigomuller0018.sobramar.categoria;

import static io.github.rodrigomuller0018.sobramar.ClienteDeTeste.comCsrf;
import static org.hamcrest.Matchers.hasItem;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoriaIntegracaoTest {

    private static final String BASE = "/api/v1/categorias";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutenticacaoService servico;

    @Test
    void contaNovaDeveNascerComAListaPadrao() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);

        mvc.perform(get(BASE).cookie(sessao).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(10))
                .andExpect(jsonPath("$.content[*].nome", hasItem("Moradia")));
    }

    @Test
    void deveCriarRenomearERecusarDuplicada() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);

        String local = mvc.perform(post(BASE)
                        .cookie(sessao)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pets\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(post(BASE)
                        .cookie(sessao)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"MORADIA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_DUPLICADA"));

        mvc.perform(put(local)
                        .cookie(sessao)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Animais\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Animais"));

        mvc.perform(delete(local).cookie(sessao).with(comCsrf())).andExpect(status().isNoContent());
        mvc.perform(get(local).cookie(sessao)).andExpect(jsonPath("$.arquivada").value(true));
        mvc.perform(get(BASE).cookie(sessao).param("size", "100"))
                .andExpect(jsonPath("$.page.totalElements").value(10));
        mvc.perform(get(BASE).cookie(sessao).param("size", "100").param("arquivadas", "true"))
                .andExpect(jsonPath("$.page.totalElements").value(11));
    }
}
