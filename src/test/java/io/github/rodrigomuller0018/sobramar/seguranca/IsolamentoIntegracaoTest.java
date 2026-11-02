package io.github.rodrigomuller0018.sobramar.seguranca;

import static io.github.rodrigomuller0018.sobramar.ClienteDeTeste.comCsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rodrigomuller0018.sobramar.ClienteDeTeste;
import io.github.rodrigomuller0018.sobramar.TestcontainersConfiguration;
import io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao.AutenticacaoService;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** O risco número um do produto: uma conta nunca pode ver ou alterar os dados de outra. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IsolamentoIntegracaoTest {

    /** Únicas rotas da API acessíveis sem sessão. Acrescentar aqui exige justificativa na revisão. */
    private static final Set<String> ROTAS_PUBLICAS = Set.of(
            "POST /api/v1/autenticacao/entrar",
            "POST /api/v1/autenticacao/segundo-fator",
            "POST /api/v1/autenticacao/renovar");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutenticacaoService servico;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping rotas;

    @Test
    void deveExigirSessaoEmTodaRotaDaApi() throws Exception {
        List<String> semProtecao = new ArrayList<>();
        for (var mapeamento : rotas.getHandlerMethods().keySet()) {
            for (String caminho : mapeamento.getPatternValues()) {
                for (RequestMethod metodo : mapeamento.getMethodsCondition().getMethods()) {
                    String rota = metodo + " " + caminho;
                    if (!caminho.startsWith("/api/") || ROTAS_PUBLICAS.contains(rota)) {
                        continue;
                    }
                    String url = caminho.replace("{id}", UUID.randomUUID().toString());
                    int resposta = mvc.perform(request(HttpMethod.valueOf(metodo.name()), url)
                                    .with(comCsrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                    if (resposta != 401) {
                        semProtecao.add(rota + " -> " + resposta);
                    }
                }
            }
        }
        assertThat(semProtecao).isEmpty();
    }

    @Test
    void naoDeveLerNemAlterarNemArquivarPessoaDeOutraConta() throws Exception {
        Cookie[] dona = ClienteDeTeste.sessao(mvc, servico);
        Cookie[] outra = ClienteDeTeste.sessao(mvc, servico);
        String local = mvc.perform(post("/api/v1/pessoas")
                        .cookie(dona)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apelido\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(get(local).cookie(outra))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
        mvc.perform(put(local)
                        .cookie(outra)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apelido\":\"Invadido\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(local).cookie(outra).with(comCsrf())).andExpect(status().isNotFound());

        mvc.perform(get(local).cookie(dona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apelido").value("Ana"))
                .andExpect(jsonPath("$.arquivada").value(false));
    }

    @Test
    void deveListarSoADaPropriaContaEPermitirOMesmoApelidoEmContasDiferentes() throws Exception {
        Cookie[] primeira = ClienteDeTeste.sessao(mvc, servico);
        Cookie[] segunda = ClienteDeTeste.sessao(mvc, servico);
        for (Cookie[] sessao : List.of(primeira, segunda)) {
            mvc.perform(post("/api/v1/pessoas")
                            .cookie(sessao)
                            .with(comCsrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"apelido\":\"Ana\"}"))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/v1/pessoas").cookie(primeira))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }
}
