package io.github.rodrigomuller0018.sobramar.carteira;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class CarteiraIntegracaoTest {

    private static final String CARTEIRAS = "/api/v1/carteiras";
    private static final String CARTOES = "/api/v1/cartoes";
    private static final String CARTAO_AZUL =
            "{\"nome\":\"Azul\",\"diaFechamento\":25,\"diaVencimento\":5,\"limiteCentavos\":500000}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutenticacaoService servico;

    @Test
    void deveGuardarOSaldoSemPerderCentavos() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);

        String local = enviar(
                        post(CARTEIRAS),
                        sessao,
                        "{\"tipo\":\"CONTA\",\"nome\":\"Principal\",\"saldoInicialCentavos\":-12345}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(get(local).cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoInicialCentavos").value(-12345))
                .andExpect(jsonPath("$.tipo").value("CONTA"));
    }

    @Test
    void naoDeveMudarOTipoDaCarteiraMasDeveArquivar() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);
        String local = enviar(
                        post(CARTEIRAS), sessao, "{\"tipo\":\"VALE\",\"nome\":\"Refeição\",\"saldoInicialCentavos\":0}")
                .andReturn()
                .getResponse()
                .getHeader("Location");

        enviar(put(local), sessao, "{\"tipo\":\"CONTA\",\"nome\":\"Refeição\",\"saldoInicialCentavos\":0}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("CARTEIRA_TIPO_IMUTAVEL"));
        enviar(put(local), sessao, "{\"tipo\":\"VALE\",\"nome\":\"Alimentação\",\"saldoInicialCentavos\":100}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Alimentação"));

        mvc.perform(delete(local).cookie(sessao).with(comCsrf())).andExpect(status().isNoContent());
        mvc.perform(get(CARTEIRAS).cookie(sessao))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get(CARTEIRAS).cookie(sessao).param("arquivadas", "true"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void deveCriarEEditarCartaoComLimiteOpcional() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);
        String local = enviar(post(CARTOES), sessao, CARTAO_AZUL)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limiteCentavos").value(500000))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        enviar(put(local), sessao, "{\"nome\":\"Azul\",\"diaFechamento\":10,\"diaVencimento\":20}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaFechamento").value(10))
                .andExpect(jsonPath("$.limiteCentavos").doesNotExist());

        mvc.perform(delete(local).cookie(sessao).with(comCsrf())).andExpect(status().isNoContent());
        mvc.perform(get(CARTOES).cookie(sessao))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get(CARTOES).cookie(sessao).param("arquivadas", "true"))
                .andExpect(jsonPath("$.content[0].arquivado").value(true));
    }

    @ParameterizedTest
    @CsvSource({"32, 5", "25, 0"})
    void deveRecusarDiasInvalidos(int fechamento, int vencimento) throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);

        enviar(
                        post(CARTOES),
                        sessao,
                        "{\"nome\":\"X\",\"diaFechamento\":%d,\"diaVencimento\":%d}".formatted(fechamento, vencimento))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("DIA_INVALIDO"));
    }

    @Test
    void deveRecusarLimiteNegativo() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);

        enviar(post(CARTOES), sessao, "{\"nome\":\"X\",\"diaFechamento\":25,\"diaVencimento\":5,\"limiteCentavos\":-1}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("VALOR_INVALIDO"));
    }

    @Test
    void deveSugerirAFaturaPelaDataDaCompra() throws Exception {
        Cookie[] sessao = ClienteDeTeste.sessao(mvc, servico);
        String local = enviar(post(CARTOES), sessao, CARTAO_AZUL)
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(get(local + "/fatura-sugerida").cookie(sessao).param("dataCompra", "2026-11-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencia").value("2026-12"))
                .andExpect(jsonPath("$.fechamento").value("2026-11-25"))
                .andExpect(jsonPath("$.vencimento").value("2026-12-05"));
        mvc.perform(get(local + "/fatura-sugerida").cookie(sessao).param("dataCompra", "2026-11-26"))
                .andExpect(jsonPath("$.competencia").value("2027-01"));
    }

    private ResultActions enviar(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requisicao,
            Cookie[] sessao,
            String corpo)
            throws Exception {
        return mvc.perform(requisicao
                .cookie(sessao)
                .with(comCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
