package io.github.rodrigomuller0018.sobramar.autenticacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TotpTest {

    /** "12345678901234567890" em Base32: o segredo dos vetores do RFC 6238. */
    private static final String SEGREDO_RFC = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @ParameterizedTest
    @CsvSource({"59, 287082", "1111111109, 081804", "1111111111, 050471", "1234567890, 005924", "2000000000, 279037"})
    void deveGerarOsCodigosDosVetoresDoRfc(long segundos, String esperado) {
        assertThat(Totp.codigo(SEGREDO_RFC, Instant.ofEpochSecond(segundos))).isEqualTo(esperado);
    }

    @Test
    void deveAceitarUmPassoDeDiferencaParaCompensarORelogio() {
        Instant agora = Instant.ofEpochSecond(1_000_000);

        assertThat(Totp.validar(SEGREDO_RFC, Totp.codigo(SEGREDO_RFC, agora.minusSeconds(30)), agora, null))
                .isPresent();
        assertThat(Totp.validar(SEGREDO_RFC, Totp.codigo(SEGREDO_RFC, agora.plusSeconds(30)), agora, null))
                .isPresent();
    }

    @Test
    void deveRecusarCodigoForaDaJanela() {
        Instant agora = Instant.ofEpochSecond(1_000_000);

        assertThat(Totp.validar(SEGREDO_RFC, Totp.codigo(SEGREDO_RFC, agora.minusSeconds(120)), agora, null))
                .isEmpty();
    }

    @Test
    void deveRecusarCodigoJaUsado() {
        Instant agora = Instant.ofEpochSecond(1_000_000);
        String codigo = Totp.codigo(SEGREDO_RFC, agora);
        long passo = Totp.validar(SEGREDO_RFC, codigo, agora, null).orElseThrow();

        assertThat(Totp.validar(SEGREDO_RFC, codigo, agora, passo)).isEmpty();
    }

    @Test
    void deveGerarSegredoQueFuncionaDeIdaEVolta() {
        String segredo = Totp.novoSegredoBase32();
        Instant agora = Instant.ofEpochSecond(1_000_000);

        assertThat(segredo).hasSize(32).matches("[A-Z2-7]+");
        assertThat(Totp.validar(segredo, Totp.codigo(segredo, agora), agora, null))
                .isPresent();
    }
}
