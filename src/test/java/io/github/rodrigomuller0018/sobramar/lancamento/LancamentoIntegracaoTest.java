package io.github.rodrigomuller0018.sobramar.lancamento;

import static io.github.rodrigomuller0018.sobramar.ClienteDeTeste.comCsrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.rodrigomuller0018.sobramar.ClienteDeTeste;
import io.github.rodrigomuller0018.sobramar.TestcontainersConfiguration;
import io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao.AutenticacaoService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LancamentoIntegracaoTest {

    private static final String BASE = "/api/v1/lancamentos";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutenticacaoService servico;

    private Cookie[] sessao;
    private String contaId;
    private String outraId;
    private String valeId;

    @BeforeEach
    void preparar() throws Exception {
        sessao = ClienteDeTeste.sessao(mvc, servico);
        contaId = criar("/api/v1/carteiras", "{\"tipo\":\"CONTA\",\"nome\":\"A\",\"saldoInicialCentavos\":0}");
        outraId = criar("/api/v1/carteiras", "{\"tipo\":\"DINHEIRO\",\"nome\":\"B\",\"saldoInicialCentavos\":0}");
        valeId = criar("/api/v1/carteiras", "{\"tipo\":\"VALE\",\"nome\":\"V\",\"saldoInicialCentavos\":0}");
    }

    private String gasto(String data, long valor) {
        return "{\"tipo\":\"GASTO\",\"descricao\":\"Almoço\",\"valorCentavos\":%d,\"data\":\"%s\",\"carteiraId\":\"%s\"}"
                .formatted(valor, data, contaId);
    }

    @Test
    void deveCriarEditarIgnorarEExcluir() throws Exception {
        String id = criar(BASE, gasto("2027-01-12", 3500));

        enviar(put(BASE + "/" + id), gasto("2027-01-13", 4000).replace("}", ",\"ignorado\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorCentavos").value(4000))
                .andExpect(jsonPath("$.ignorado").value(true));

        mvc.perform(delete(BASE + "/" + id).cookie(sessao).with(comCsrf())).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/" + id).cookie(sessao)).andExpect(status().isNotFound());
    }

    @Test
    void deveFiltrarPorPeriodoECarteira() throws Exception {
        criar(BASE, gasto("2027-01-12", 100));
        criar(BASE, gasto("2027-02-12", 200));

        mvc.perform(get(BASE).cookie(sessao).param("de", "2027-02-01").param("ate", "2027-02-28"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].valorCentavos").value(200));
        mvc.perform(get(BASE).cookie(sessao).param("carteiraId", contaId))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get(BASE).cookie(sessao).param("carteiraId", outraId))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void transferenciaDeveExigirDestinoDiferenteENaoMisturarVale() throws Exception {
        String base = "{\"tipo\":\"TRANSFERENCIA\",\"descricao\":\"T\",\"valorCentavos\":100,\"data\":\"2027-01-12\","
                + "\"carteiraId\":\"%s\"".formatted(contaId);

        enviar(post(BASE), base + ",\"carteiraDestinoId\":\"%s\"}".formatted(outraId))
                .andExpect(status().isCreated());
        enviar(post(BASE), base + "}").andExpect(jsonPath("$.codigo").value("TRANSFERENCIA_INVALIDA"));
        enviar(post(BASE), base + ",\"carteiraDestinoId\":\"%s\"}".formatted(contaId))
                .andExpect(jsonPath("$.codigo").value("TRANSFERENCIA_INVALIDA"));
        enviar(post(BASE), base + ",\"carteiraDestinoId\":\"%s\"}".formatted(valeId))
                .andExpect(jsonPath("$.codigo").value("CARTEIRA_INCOMPATIVEL"));
    }

    @Test
    void deveRecusarValorInvalido() throws Exception {
        enviar(post(BASE), gasto("2027-01-12", 0))
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"));
    }

    private String criar(String caminho, String corpo) throws Exception {
        String resposta = enviar(post(caminho), corpo)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, String corpo) throws Exception {
        return mvc.perform(requisicao
                .cookie(sessao)
                .with(comCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
