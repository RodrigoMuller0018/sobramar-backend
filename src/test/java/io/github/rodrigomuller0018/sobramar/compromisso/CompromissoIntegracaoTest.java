package io.github.rodrigomuller0018.sobramar.compromisso;

import static io.github.rodrigomuller0018.sobramar.ClienteDeTeste.comCsrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

/** Compromissos, ocorrências e pagamentos de ponta a ponta, com PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CompromissoIntegracaoTest {

    private static final String BASE = "/api/v1/compromissos";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutenticacaoService servico;

    private Cookie[] sessao;
    private String carteiraId;
    private String valeId;
    private String cartaoId;

    @BeforeEach
    void preparar() throws Exception {
        sessao = ClienteDeTeste.sessao(mvc, servico);
        carteiraId = criar("/api/v1/carteiras", "{\"tipo\":\"CONTA\",\"nome\":\"C\",\"saldoInicialCentavos\":0}");
        valeId = criar("/api/v1/carteiras", "{\"tipo\":\"VALE\",\"nome\":\"V\",\"saldoInicialCentavos\":0}");
        cartaoId = criar(
                "/api/v1/cartoes",
                "{\"nome\":\"Azul\",\"diaFechamento\":25,\"diaVencimento\":5,\"limiteCentavos\":1000000}");
    }

    private String boleto(int parcelas, long total) {
        return ("{\"natureza\":\"SAIDA\",\"forma\":\"PARCELADO\",\"certeza\":\"CERTO\",\"descricao\":\"Boleto\","
                        + "\"valorTotalCentavos\":%d,\"parcelas\":%d,\"dataInicio\":\"2027-02-10\",\"carteiraId\":\"%s\"}")
                .formatted(total, parcelas, carteiraId);
    }

    private String compraNoCartao(String extra) {
        return ("{\"natureza\":\"SAIDA\",\"forma\":\"PARCELADO\",\"certeza\":\"CERTO\",\"descricao\":\"Notebook\","
                        + "\"valorTotalCentavos\":120000,\"parcelas\":12,\"dataInicio\":\"2026-11-24\",\"cartaoId\":\"%s\"%s}")
                .formatted(cartaoId, extra);
    }

    @Test
    void deveCalcularAsParcelasDoBoletoAoConsultar() throws Exception {
        String id = criar(BASE, boleto(36, 360_000));

        mvc.perform(get(BASE + "/" + id).cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocorrencias.length()").value(36))
                .andExpect(jsonPath("$.ocorrencias[0].vencimento").value("2027-02-10"))
                .andExpect(jsonPath("$.ocorrencias[35].vencimento").value("2030-01-10"))
                .andExpect(jsonPath("$.ocorrencias[0].situacao").value("PREVISTO"));
    }

    @Test
    void deveCalcularAsFaturasDaCompraEMostrarCompraEVencimentoSeparados() throws Exception {
        String id = criar(BASE, compraNoCartao(""));

        mvc.perform(get(BASE + "/" + id).cookie(sessao))
                .andExpect(jsonPath("$.ocorrencias[0].vencimento").value("2026-12-05"))
                .andExpect(jsonPath("$.ocorrencias[1].vencimento").value("2027-01-05"))
                .andExpect(jsonPath("$.ocorrencias[1].dataCompra").value("2026-11-24"))
                .andExpect(jsonPath("$.ocorrencias[1].competencia").value("2027-01"));
    }

    @Test
    void deveAceitarFaturaEscolhidaEAvisarQuandoNaoCombina() throws Exception {
        enviar(post(BASE), compraNoCartao(",\"faturaInicial\":\"2026-10\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avisos[0].codigo").value("FATURA_INCOMPATIVEL_COM_DATA"));
        enviar(post(BASE), compraNoCartao(",\"faturaInicial\":\"2026-12\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avisos.length()").value(0));
    }

    @Test
    void deveValidarAsRegrasDoPlano() throws Exception {
        enviar(post(BASE), boleto(121, 1000))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("BOLETO_LIMITE_PARCELAS"));
        enviar(post(BASE), compraNoCartao("").replace("\"parcelas\":12", "\"parcelas\":49"))
                .andExpect(jsonPath("$.codigo").value("CARTAO_LIMITE_PARCELAS"));
        enviar(post(BASE), boleto(3, 0))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"));
        enviar(post(BASE), boleto(3, 100).replace("\"carteiraId\":\"" + carteiraId + "\"", "\"carteiraId\":null"))
                .andExpect(jsonPath("$.codigo").value("ORIGEM_OBRIGATORIA"));
        enviar(post(BASE), compraNoCartao(",\"carteiraId\":\"" + carteiraId + "\""))
                .andExpect(jsonPath("$.codigo").value("ORIGEM_DUPLA"));
        enviar(post(BASE), compraNoCartao("").replace("\"SAIDA\"", "\"ENTRADA\""))
                .andExpect(jsonPath("$.codigo").value("CARTEIRA_INCOMPATIVEL"));
    }

    @Test
    void deveEditarUmaParcelaComEscopo() throws Exception {
        String id = criar(BASE, boleto(4, 40_000));
        String ocorrencias = BASE + "/" + id + "/ocorrencias/";

        enviar(patch(ocorrencias + "2"), "{\"escopo\":\"SOMENTE_ESTA\",\"valorCentavos\":12000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorCentavos").value(12000));
        mvc.perform(get(BASE + "/" + id).cookie(sessao))
                .andExpect(jsonPath("$.ocorrencias[0].valorCentavos").value(10000))
                .andExpect(jsonPath("$.ocorrencias[1].valorCentavos").value(12000))
                .andExpect(jsonPath("$.ocorrencias[2].valorCentavos").value(10000));

        enviar(patch(ocorrencias + "3"), "{\"escopo\":\"ESTA_E_FUTURAS\",\"valorCentavos\":15000}")
                .andExpect(status().isOk());
        mvc.perform(get(BASE + "/" + id).cookie(sessao))
                .andExpect(jsonPath("$.ocorrencias[1].valorCentavos").value(12000))
                .andExpect(jsonPath("$.ocorrencias[3].valorCentavos").value(15000));

        enviar(patch(ocorrencias + "4"), "{\"escopo\":\"TODAS\",\"valorCentavos\":9000}")
                .andExpect(status().isOk());
        mvc.perform(get(BASE + "/" + id).cookie(sessao))
                .andExpect(jsonPath("$.ocorrencias[0].valorCentavos").value(9000))
                .andExpect(jsonPath("$.ocorrencias[1].valorCentavos").value(9000))
                .andExpect(jsonPath("$.ocorrencias[3].valorCentavos").value(9000));
    }

    @Test
    void deveRecusarEdicoesInvalidasDeParcela() throws Exception {
        String id = criar(BASE, boleto(2, 20_000));
        String ocorrencia = BASE + "/" + id + "/ocorrencias/";

        enviar(patch(ocorrencia + "1"), "{\"escopo\":\"TODAS\",\"ignorada\":true}")
                .andExpect(jsonPath("$.codigo").value("ESCOPO_INVALIDO"));
        enviar(patch(ocorrencia + "1"), "{\"escopo\":\"SOMENTE_ESTA\",\"valorCentavos\":0}")
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"));
        enviar(patch(ocorrencia + "1"), "{\"escopo\":\"SOMENTE_ESTA\",\"fatura\":\"2027-03\"}")
                .andExpect(jsonPath("$.codigo").value("CARTEIRA_INCOMPATIVEL"));
        enviar(patch(ocorrencia + "3"), "{\"escopo\":\"SOMENTE_ESTA\",\"valorCentavos\":100}")
                .andExpect(status().isNotFound());
    }

    @Test
    void realizadoDeveSubstituirOPrevistoEBloquearEdicaoEExclusao() throws Exception {
        String id = criar(BASE, boleto(2, 20_000));
        String pagamento = BASE + "/" + id + "/ocorrencias/1/pagamento";
        String corpo = "{\"data\":\"2027-02-11\",\"valorCentavos\":10500,\"carteiraId\":\"%s\"}".formatted(carteiraId);

        enviar(post(pagamento), corpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("GASTO"));

        mvc.perform(get(BASE + "/" + id).cookie(sessao))
                .andExpect(jsonPath("$.ocorrencias[0].situacao").value("REALIZADO"))
                .andExpect(jsonPath("$.ocorrencias[0].valorCentavos").value(10500))
                .andExpect(jsonPath("$.ocorrencias[1].situacao").value("PREVISTO"));

        enviar(post(pagamento), corpo)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OCORRENCIA_JA_PAGA"));
        enviar(patch(BASE + "/" + id + "/ocorrencias/1"), "{\"escopo\":\"SOMENTE_ESTA\",\"valorCentavos\":100}")
                .andExpect(status().isConflict());
        mvc.perform(delete(BASE + "/" + id).cookie(sessao).with(comCsrf())).andExpect(status().isConflict());

        mvc.perform(delete(pagamento).cookie(sessao).with(comCsrf())).andExpect(status().isNoContent());
        mvc.perform(delete(BASE + "/" + id).cookie(sessao).with(comCsrf())).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/" + id).cookie(sessao)).andExpect(status().isNotFound());
    }

    @Test
    void parcelaDoCartaoSoSePagaPelaFaturaEPagarNaoContaDeNovo() throws Exception {
        String id = criar(BASE, compraNoCartao(""));
        String corpo = "{\"data\":\"2026-12-05\",\"valorCentavos\":10000,\"carteiraId\":\"%s\"}".formatted(carteiraId);

        enviar(post(BASE + "/" + id + "/ocorrencias/1/pagamento"), corpo)
                .andExpect(jsonPath("$.codigo").value("OCORRENCIA_EM_CARTAO"));

        String fatura = "/api/v1/cartoes/" + cartaoId + "/faturas/2026-12/pagamento";
        enviar(post(fatura), corpo).andExpect(status().isCreated());
        enviar(post(fatura), corpo)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("FATURA_JA_PAGA"));

        mvc.perform(get(BASE + "/" + id).cookie(sessao))
                .andExpect(jsonPath("$.ocorrencias[0].situacao").value("REALIZADO"))
                .andExpect(jsonPath("$.ocorrencias[1].situacao").value("PREVISTO"));
        mvc.perform(get("/api/v1/lancamentos").cookie(sessao))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        mvc.perform(delete(fatura).cookie(sessao).with(comCsrf())).andExpect(status().isNoContent());
        mvc.perform(delete(fatura).cookie(sessao).with(comCsrf())).andExpect(status().isNotFound());
    }

    @Test
    void faturaNaoSePagaComValeEValeNaoPagaBoletoDaConta() throws Exception {
        String id = criar(BASE, boleto(1, 1000));
        String comVale = "{\"data\":\"2027-02-10\",\"valorCentavos\":1000,\"carteiraId\":\"%s\"}".formatted(valeId);

        enviar(post("/api/v1/cartoes/" + cartaoId + "/faturas/2026-12/pagamento"), comVale)
                .andExpect(jsonPath("$.codigo").value("CARTEIRA_INCOMPATIVEL"));
        enviar(post(BASE + "/" + id + "/ocorrencias/1/pagamento"), comVale)
                .andExpect(jsonPath("$.codigo").value("CARTEIRA_INCOMPATIVEL"));
    }

    @Test
    void oLimiteUsadoDeveSomarTodasAsParcelasEDescontarFaturaPaga() throws Exception {
        criar(BASE, compraNoCartao(""));
        String local = "/api/v1/cartoes/" + cartaoId;

        mvc.perform(get(local).cookie(sessao))
                .andExpect(jsonPath("$.limiteUsadoCentavos").value(120000))
                .andExpect(jsonPath("$.limiteDisponivelCentavos").value(880000));

        enviar(
                        post(local + "/faturas/2026-12/pagamento"),
                        "{\"data\":\"2026-12-05\",\"valorCentavos\":10000,\"carteiraId\":\"%s\"}".formatted(carteiraId))
                .andExpect(status().isCreated());

        mvc.perform(get(local).cookie(sessao))
                .andExpect(jsonPath("$.limiteUsadoCentavos").value(110000));
    }

    @Test
    void deveCancelarAtualizarEFiltrar() throws Exception {
        String id = criar(BASE, boleto(3, 30_000));

        enviar(post(BASE + "/" + id + "/cancelamento"), "{\"aPartirDe\":\"2027-03-01\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceladoAPartirDe").value("2027-03-01"));
        mvc.perform(get(BASE + "/" + id).cookie(sessao))
                .andExpect(jsonPath("$.ocorrencias.length()").value(1));
        mvc.perform(get(BASE).cookie(sessao))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get(BASE).cookie(sessao).param("ativos", "false").param("forma", "PARCELADO"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        enviar(put(BASE + "/" + id), boleto(6, 60_000))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.canceladoAPartirDe").value("2027-03-01"));
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
