package io.github.rodrigomuller0018.sobramar.seguranca;

import static io.github.rodrigomuller0018.sobramar.ClienteDeTeste.comCsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.rodrigomuller0018.sobramar.ClienteDeTeste;
import io.github.rodrigomuller0018.sobramar.TestcontainersConfiguration;
import io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao.AutenticacaoService;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
                    String url = caminho.replaceAll("\\{[^}]+}", "1");
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("recursos")
    void naoDeveLerNemAlterarNemArquivarRecursoDeOutraConta(String base, String corpo) throws Exception {
        Cookie[] dona = ClienteDeTeste.sessao(mvc, servico);
        Cookie[] outra = ClienteDeTeste.sessao(mvc, servico);
        String local = mvc.perform(post(base)
                        .cookie(dona)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        String original = mvc.perform(get(local).cookie(dona))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mvc.perform(get(local).cookie(outra))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
        mvc.perform(put(local)
                        .cookie(outra)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isNotFound());
        mvc.perform(delete(local).cookie(outra).with(comCsrf())).andExpect(status().isNotFound());

        mvc.perform(get(local).cookie(dona)).andExpect(content().json(original));
    }

    static Stream<Arguments> recursos() {
        return Stream.of(
                Arguments.of("/api/v1/pessoas", "{\"apelido\":\"Ana\"}"),
                Arguments.of("/api/v1/categorias", "{\"nome\":\"Pets\"}"),
                Arguments.of(
                        "/api/v1/carteiras",
                        "{\"tipo\":\"CONTA\",\"nome\":\"Principal\",\"saldoInicialCentavos\":100}"),
                Arguments.of("/api/v1/cartoes", "{\"nome\":\"Azul\",\"diaFechamento\":25,\"diaVencimento\":5}"));
    }

    @Test
    void naoDeveSugerirFaturaDeCartaoDeOutraConta() throws Exception {
        Cookie[] dona = ClienteDeTeste.sessao(mvc, servico);
        Cookie[] outra = ClienteDeTeste.sessao(mvc, servico);
        String local = mvc.perform(post("/api/v1/cartoes")
                        .cookie(dona)
                        .with(comCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Azul\",\"diaFechamento\":25,\"diaVencimento\":5}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(get(local + "/fatura-sugerida").cookie(outra).param("dataCompra", "2026-11-24"))
                .andExpect(status().isNotFound());
    }

    @Test
    void naoDeveUsarNemPagarNadaDeOutraConta() throws Exception {
        Cookie[] dona = ClienteDeTeste.sessao(mvc, servico);
        Cookie[] outra = ClienteDeTeste.sessao(mvc, servico);
        String carteiraDaDona =
                idCriado(dona, "/api/v1/carteiras", "{\"tipo\":\"CONTA\",\"nome\":\"C\",\"saldoInicialCentavos\":0}");
        String carteiraDaOutra =
                idCriado(outra, "/api/v1/carteiras", "{\"tipo\":\"CONTA\",\"nome\":\"C\",\"saldoInicialCentavos\":0}");
        String cartaoDaDona =
                idCriado(dona, "/api/v1/cartoes", "{\"nome\":\"A\",\"diaFechamento\":25,\"diaVencimento\":5}");
        String pessoaDaDona = idCriado(dona, "/api/v1/pessoas", "{\"apelido\":\"Ana\"}");
        String boleto = "{\"natureza\":\"SAIDA\",\"forma\":\"PARCELADO\",\"certeza\":\"CERTO\",\"descricao\":\"B\","
                + "\"valorTotalCentavos\":1000,\"parcelas\":2,\"dataInicio\":\"2027-01-10\",\"carteiraId\":\"%s\"%s}";
        String compromissoDaDona = idCriado(dona, "/api/v1/compromissos", boleto.formatted(carteiraDaDona, ""));
        String lancamentoDaDona = idCriado(
                dona,
                "/api/v1/lancamentos",
                "{\"tipo\":\"GASTO\",\"descricao\":\"L\",\"valorCentavos\":100,\"data\":\"2027-01-10\",\"carteiraId\":\"%s\"}"
                        .formatted(carteiraDaDona));
        String pagamento = "{\"data\":\"2027-01-10\",\"valorCentavos\":100,\"carteiraId\":\"%s\"}";

        // Referências a dados de outra conta dentro do corpo
        enviar(outra, post("/api/v1/compromissos"), boleto.formatted(carteiraDaDona, ""))
                .andExpect(status().isNotFound());
        enviar(
                        outra,
                        post("/api/v1/compromissos"),
                        boleto.formatted(carteiraDaOutra, ",\"pessoaId\":\"%s\"".formatted(pessoaDaDona)))
                .andExpect(status().isNotFound());
        enviar(
                        outra,
                        post("/api/v1/lancamentos"),
                        "{\"tipo\":\"GASTO\",\"descricao\":\"L\",\"valorCentavos\":100,\"data\":\"2027-01-10\",\"carteiraId\":\"%s\"}"
                                .formatted(carteiraDaDona))
                .andExpect(status().isNotFound());

        // Recursos de outra conta na URL
        String compromisso = "/api/v1/compromissos/" + compromissoDaDona;
        mvc.perform(get(compromisso).cookie(outra)).andExpect(status().isNotFound());
        enviar(outra, patch(compromisso + "/ocorrencias/1"), "{\"escopo\":\"SOMENTE_ESTA\",\"valorCentavos\":1}")
                .andExpect(status().isNotFound());
        enviar(outra, post(compromisso + "/cancelamento"), "{\"aPartirDe\":\"2027-01-01\"}")
                .andExpect(status().isNotFound());
        enviar(outra, post(compromisso + "/ocorrencias/1/pagamento"), pagamento.formatted(carteiraDaOutra))
                .andExpect(status().isNotFound());
        mvc.perform(delete(compromisso + "/ocorrencias/1/pagamento")
                        .cookie(outra)
                        .with(comCsrf()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(compromisso).cookie(outra).with(comCsrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/lancamentos/" + lancamentoDaDona).cookie(outra))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/lancamentos/" + lancamentoDaDona)
                        .cookie(outra)
                        .with(comCsrf()))
                .andExpect(status().isNotFound());
        enviar(
                        outra,
                        post("/api/v1/cartoes/" + cartaoDaDona + "/faturas/2027-01/pagamento"),
                        pagamento.formatted(carteiraDaOutra))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/cartoes/" + cartaoDaDona + "/faturas/2027-01/pagamento")
                        .cookie(outra)
                        .with(comCsrf()))
                .andExpect(status().isNotFound());

        // Nada apareceu nas listas da outra conta, e a dona segue intacta
        mvc.perform(get("/api/v1/compromissos").cookie(outra))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/v1/lancamentos").cookie(outra))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get(compromisso).cookie(dona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocorrencias[0].valorCentavos").value(500));
    }

    private String idCriado(Cookie[] sessao, String caminho, String corpo) throws Exception {
        String resposta = enviar(sessao, post(caminho), corpo)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions enviar(Cookie[] sessao, MockHttpServletRequestBuilder requisicao, String corpo)
            throws Exception {
        return mvc.perform(requisicao
                .cookie(sessao)
                .with(comCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
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
