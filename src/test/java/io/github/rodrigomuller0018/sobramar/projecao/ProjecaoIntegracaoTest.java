package io.github.rodrigomuller0018.sobramar.projecao;

import static io.github.rodrigomuller0018.sobramar.ClienteDeTeste.comCsrf;
import static org.hamcrest.Matchers.hasItem;
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
import java.time.LocalDate;
import java.time.ZoneId;
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

/** Da API ao banco. A conta em si é conferida em {@code ExemploDeReferenciaTest}. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProjecaoIntegracaoTest {

    private static final String COMPROMISSOS = "/api/v1/compromissos";
    private static final String PROJECAO = "/api/v1/projecao";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutenticacaoService servico;

    private Cookie[] sessao;
    private String carteiraId;
    private String cartaoId;

    @BeforeEach
    void preparar() throws Exception {
        sessao = ClienteDeTeste.sessao(mvc, servico);
        carteiraId = criar("/api/v1/carteiras", "{\"tipo\":\"CONTA\",\"nome\":\"C\",\"saldoInicialCentavos\":0}");
        cartaoId = criar("/api/v1/cartoes", "{\"nome\":\"Azul\",\"diaFechamento\":25,\"diaVencimento\":5}");
    }

    private String boleto(int parcelas, long total, String data) {
        return ("{\"natureza\":\"SAIDA\",\"forma\":\"PARCELADO\",\"certeza\":\"CERTO\",\"descricao\":\"Boleto\","
                        + "\"valorTotalCentavos\":%d,\"parcelas\":%d,\"dataInicio\":\"%s\",\"carteiraId\":\"%s\"}")
                .formatted(total, parcelas, data, carteiraId);
    }

    private String salario() {
        return ("{\"natureza\":\"ENTRADA\",\"forma\":\"RECORRENTE\",\"certeza\":\"CERTO\",\"descricao\":\"Salário\","
                        + "\"valorCentavos\":500000,\"dataInicio\":\"2027-01-05\",\"carteiraId\":\"%s\"}")
                .formatted(carteiraId);
    }

    @Test
    void deveDevolverOsMesesComSobraENomes() throws Exception {
        criar(COMPROMISSOS, salario());
        criar(COMPROMISSOS, boleto(2, 20_000, "2027-02-10"));
        criar(
                COMPROMISSOS,
                ("{\"natureza\":\"SAIDA\",\"forma\":\"PARCELADO\",\"certeza\":\"ESTIMADO\",\"descricao\":\"TV\","
                                + "\"valorTotalCentavos\":30000,\"parcelas\":1,\"dataInicio\":\"2027-02-01\",\"cartaoId\":\"%s\"}")
                        .formatted(cartaoId));

        mvc.perform(get(PROJECAO).cookie(sessao).param("de", "2027-01").param("ate", "2027-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meses.length()").value(3))
                .andExpect(jsonPath("$.meses[0].competencia").value("2027-01"))
                .andExpect(jsonPath("$.meses[0].sobraCentavos").value(500000))
                .andExpect(jsonPath("$.meses[1].compromissosCentavos").value(10000))
                .andExpect(jsonPath("$.meses[2].sobraCentavos").value(460000))
                .andExpect(jsonPath("$.meses[2].contemEstimativa").value(true))
                .andExpect(jsonPath("$.meses[2].faturas[0].nomeCartao").value("Azul"))
                .andExpect(jsonPath("$.meses[2].faturas[0].totalCentavos").value(30000))
                .andExpect(jsonPath("$.meses[2].itens[?(@.descricao == 'TV')].dataCompra", hasItem("2027-02-01")));
    }

    @Test
    void deveRecusarPeriodoInvalido() throws Exception {
        mvc.perform(get(PROJECAO).cookie(sessao).param("de", "2027-03").param("ate", "2027-01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("PERIODO_INVALIDO"));
        mvc.perform(get(PROJECAO).cookie(sessao).param("de", "2027-01").param("ate", "2029-01"))
                .andExpect(jsonPath("$.codigo").value("PERIODO_INVALIDO"));
        mvc.perform(get(PROJECAO).cookie(sessao).param("de", "2027-01").param("ate", "2028-12"))
                .andExpect(status().isOk());
    }

    @Test
    void deveSimularSemGravarEDizerQuandoATerraVolta() throws Exception {
        criar(COMPROMISSOS, salario());
        String corpo = ("{\"de\":\"2027-01\",\"ate\":\"2027-03\",\"compra\":{\"valorTotalCentavos\":360000,"
                        + "\"parcelas\":36,\"dataInicio\":\"2027-02-10\",\"carteiraId\":\"%s\"}}")
                .formatted(carteiraId);

        enviar(post(PROJECAO + "/simulacao"), corpo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meses[0].diferencaCentavos").value(0))
                .andExpect(jsonPath("$.meses[1].sobraAtualCentavos").value(500000))
                .andExpect(jsonPath("$.meses[1].sobraSimuladaCentavos").value(490000))
                .andExpect(jsonPath("$.meses[1].diferencaCentavos").value(-10000))
                .andExpect(jsonPath("$.valorParcelaCentavos").value(10000))
                .andExpect(jsonPath("$.primeiroPagamento").value("2027-02-10"))
                .andExpect(jsonPath("$.ultimoPagamento").value("2030-01-10"));

        mvc.perform(get(COMPROMISSOS).cookie(sessao))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void deveSimularNoCartaoEValidarARegra() throws Exception {
        enviar(
                        post(PROJECAO + "/simulacao"),
                        ("{\"de\":\"2026-12\",\"ate\":\"2027-01\",\"compra\":{\"valorTotalCentavos\":20000,"
                                        + "\"parcelas\":2,\"dataInicio\":\"2026-11-24\",\"cartaoId\":\"%s\"}}")
                                .formatted(cartaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primeiroPagamento").value("2026-12-05"))
                .andExpect(jsonPath("$.ultimoPagamento").value("2027-01-05"));
        enviar(
                        post(PROJECAO + "/simulacao"),
                        ("{\"de\":\"2026-12\",\"ate\":\"2027-01\",\"compra\":{\"valorTotalCentavos\":20000,"
                                        + "\"parcelas\":49,\"dataInicio\":\"2026-11-24\",\"cartaoId\":\"%s\"}}")
                                .formatted(cartaoId))
                .andExpect(jsonPath("$.codigo").value("CARTAO_LIMITE_PARCELAS"));
    }

    @Test
    void deveListarPendenciasVencidasEProximasMasNaoAsPagasNemAsDistantes() throws Exception {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        String pago = criar(COMPROMISSOS, boleto(1, 1_000, hoje.minusDays(5).toString()));
        criar(COMPROMISSOS, boleto(1, 2_000, hoje.minusDays(3).toString()));
        criar(COMPROMISSOS, boleto(1, 3_000, hoje.plusDays(3).toString()));
        criar(COMPROMISSOS, boleto(1, 4_000, hoje.plusDays(20).toString()));
        enviar(
                        post(COMPROMISSOS + "/" + pago + "/ocorrencias/1/pagamento"),
                        "{\"data\":\"%s\",\"valorCentavos\":1000,\"carteiraId\":\"%s\"}".formatted(hoje, carteiraId))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/pendencias").cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].valorCentavos").value(2000))
                .andExpect(jsonPath("$.itens[0].diasAteVencimento").value(-3))
                .andExpect(jsonPath("$.itens[0].tipo").value("OCORRENCIA"))
                .andExpect(jsonPath("$.itens[1].diasAteVencimento").value(3));
        mvc.perform(get("/api/v1/pendencias").cookie(sessao).param("dias", "31"))
                .andExpect(jsonPath("$.itens.length()").value(3));
        mvc.perform(get("/api/v1/pendencias").cookie(sessao).param("dias", "32"))
                .andExpect(jsonPath("$.codigo").value("PERIODO_INVALIDO"));
    }

    @Test
    void deveMostrarAFaturaComoPendenciaAteSerPaga() throws Exception {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        criar(
                COMPROMISSOS,
                ("{\"natureza\":\"SAIDA\",\"forma\":\"PARCELADO\",\"certeza\":\"CERTO\",\"descricao\":\"TV\","
                                + "\"valorTotalCentavos\":30000,\"parcelas\":1,\"dataInicio\":\"%s\",\"cartaoId\":\"%s\"}")
                        .formatted(hoje.minusMonths(3), cartaoId));

        String resposta = mvc.perform(get("/api/v1/pendencias").cookie(sessao))
                .andExpect(jsonPath("$.itens[0].tipo").value("FATURA"))
                .andExpect(jsonPath("$.itens[0].descricao").value("Azul"))
                .andExpect(jsonPath("$.itens[0].valorCentavos").value(30000))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String competencia = JsonPath.read(resposta, "$.itens[0].competencia");

        enviar(
                        post("/api/v1/cartoes/%s/faturas/%s/pagamento".formatted(cartaoId, competencia)),
                        "{\"data\":\"%s\",\"valorCentavos\":30000,\"carteiraId\":\"%s\"}".formatted(hoje, carteiraId))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/pendencias").cookie(sessao))
                .andExpect(jsonPath("$.itens.length()").value(0));
    }

    @Test
    void comPagamentoSoDescricaoPessoaECategoriaPodemMudar() throws Exception {
        String id = criar(COMPROMISSOS, boleto(2, 20_000, "2027-02-10"));
        enviar(
                        post(COMPROMISSOS + "/" + id + "/ocorrencias/1/pagamento"),
                        "{\"data\":\"2027-02-10\",\"valorCentavos\":10000,\"carteiraId\":\"%s\"}".formatted(carteiraId))
                .andExpect(status().isCreated());

        enviar(put(COMPROMISSOS + "/" + id), boleto(6, 60_000, "2027-02-10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OCORRENCIA_JA_PAGA"));
        enviar(put(COMPROMISSOS + "/" + id), boleto(2, 20_000, "2027-02-10").replace("Boleto", "Curso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Curso"));
    }

    @Test
    void naoDeveMisturarNemUsarDadosDeOutraConta() throws Exception {
        criar(COMPROMISSOS, salario());
        Cookie[] outra = ClienteDeTeste.sessao(mvc, servico);

        mvc.perform(get(PROJECAO).cookie(outra).param("de", "2027-01").param("ate", "2027-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meses[0].receitasCentavos").value(0));
        mvc.perform(get("/api/v1/pendencias").cookie(outra).param("dias", "31"))
                .andExpect(jsonPath("$.itens.length()").value(0));

        for (String origem :
                new String[] {"\"carteiraId\":\"" + carteiraId + "\"", "\"cartaoId\":\"" + cartaoId + "\""}) {
            mvc.perform(post(PROJECAO + "/simulacao")
                            .cookie(outra)
                            .with(comCsrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"de\":\"2027-01\",\"ate\":\"2027-02\",\"compra\":{\"valorTotalCentavos\":1000,"
                                    + "\"parcelas\":1,\"dataInicio\":\"2027-01-10\"," + origem + "}}"))
                    .andExpect(status().isNotFound());
        }
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
