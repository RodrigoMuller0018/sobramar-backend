package io.github.rodrigomuller0018.sobramar.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rodrigomuller0018.sobramar.TestcontainersConfiguration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Precisa do Tomcat de verdade: o limite de idiomas não existe no MockMvc. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class IdiomasDoNavegadorIntegracaoTest {

    @LocalServerPort
    private int porta;

    /** O servidor real cria o filtro de autenticação antes do registrar de {@link TestcontainersConfiguration}. */
    @DynamicPropertySource
    static void segredos(DynamicPropertyRegistry registro) {
        registro.add("sobramar.seguranca.jwt-segredo", IdiomasDoNavegadorIntegracaoTest::chaveAleatoria);
        registro.add("sobramar.seguranca.chave-cifragem", IdiomasDoNavegadorIntegracaoTest::chaveAleatoria);
    }

    private static String chaveAleatoria() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private HttpResponse<String> entrarCom(int idiomas) throws IOException, InterruptedException {
        String cabecalho =
                IntStream.range(0, idiomas).mapToObj(i -> "x-a" + i + ";q=0.5").collect(Collectors.joining(","));
        HttpRequest requisicao = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + porta + "/api/v1/autenticacao/entrar"))
                .header("Content-Type", "application/json")
                .header("Accept-Language", cabecalho)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"ninguem@exemplo.test\",\"senha\":\"senha-errada\"}"))
                .build();
        try (HttpClient cliente = HttpClient.newHttpClient()) {
            return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void deveAtenderNavegadorComMuitosIdiomas() throws Exception {
        HttpResponse<String> resposta = entrarCom(12);

        assertThat(resposta.statusCode()).isEqualTo(401);
        assertThat(resposta.body()).contains("CREDENCIAIS_INVALIDAS");
    }

    @Test
    void deveNaoDisfarcarErroDoServidorComoSessaoVencida() throws Exception {
        HttpResponse<String> resposta = entrarCom(51);

        assertThat(resposta.statusCode()).isEqualTo(500);
        assertThat(resposta.body()).doesNotContain("NAO_AUTENTICADO", "IllegalArgumentException");
    }
}
