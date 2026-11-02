package io.github.rodrigomuller0018.sobramar.carteira.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura.Fatura;
import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RegraDeFaturaTest {

    @Nested
    class FaturaDaCompra {

        @ParameterizedTest(name = "fecha 25, vence 5: compra em {0} vence em {1}")
        @CsvSource({
            "2026-11-24, 2026-12-05, 2026-11-25, 2026-12",
            "2026-11-25, 2027-01-05, 2026-12-25, 2027-01",
            "2026-11-26, 2027-01-05, 2026-12-25, 2027-01",
            "2026-12-31, 2027-02-05, 2027-01-25, 2027-02"
        })
        void deveVencerNoMesSeguinteAoFechamentoQuandoOVencimentoEMenor(
                LocalDate compra, LocalDate vencimento, LocalDate fechamento, YearMonth competencia) {
            Fatura fatura = new RegraDeFatura(25, 5).faturaDaCompra(compra);

            assertThat(fatura).isEqualTo(new Fatura(competencia, fechamento, vencimento));
        }

        @Test
        void deveVencerNoMesmoMesQuandoOVencimentoEMaiorQueOFechamento() {
            Fatura fatura = new RegraDeFatura(3, 10).faturaDaCompra(LocalDate.parse("2027-03-02"));

            assertThat(fatura.fechamento()).isEqualTo("2027-03-03");
            assertThat(fatura.vencimento()).isEqualTo("2027-03-10");
            assertThat(fatura.competencia()).isEqualTo(YearMonth.parse("2027-03"));
        }

        @Test
        void deveVencerNoMesSeguinteQuandoOsDiasSaoIguais() {
            Fatura fatura = new RegraDeFatura(10, 10).faturaDaCompra(LocalDate.parse("2027-03-02"));

            assertThat(fatura.fechamento()).isEqualTo("2027-03-10");
            assertThat(fatura.vencimento()).isEqualTo("2027-04-10");
        }
    }

    @Nested
    class FimDeMes {

        @ParameterizedTest(name = "fecha 31: compra em {0} vai para o fechamento de {1}")
        @CsvSource({"2027-02-27, 2027-02-28", "2027-02-28, 2027-03-31", "2028-02-28, 2028-02-29"})
        void deveUsarOUltimoDiaQuandoOMesNaoTemODiaDoFechamento(LocalDate compra, LocalDate fechamento) {
            assertThat(new RegraDeFatura(31, 10).faturaDaCompra(compra).fechamento())
                    .isEqualTo(fechamento);
        }

        @Test
        void naoDeveAcumularOAjusteNoMesSeguinte() {
            RegraDeFatura regra = new RegraDeFatura(20, 31);

            assertThat(regra.faturaDaCompetencia(YearMonth.parse("2027-02")).vencimento())
                    .isEqualTo("2027-02-28");
            assertThat(regra.faturaDaCompetencia(YearMonth.parse("2027-03")).vencimento())
                    .isEqualTo("2027-03-31");
        }
    }

    @Nested
    class Parcelas {

        @Test
        void deveColocarCadaParcelaNaFaturaSeguinteAtravessandoOAno() {
            RegraDeFatura regra = new RegraDeFatura(25, 5);
            YearMonth primeira =
                    regra.faturaDaCompra(LocalDate.parse("2026-11-24")).competencia();

            assertThat(regra.faturaDaParcela(primeira, 0).vencimento()).isEqualTo("2026-12-05");
            assertThat(regra.faturaDaParcela(primeira, 1).vencimento()).isEqualTo("2027-01-05");
            assertThat(regra.faturaDaParcela(primeira, 11).vencimento()).isEqualTo("2027-11-05");
        }

        @Test
        void aCompetenciaDeveVoltarAMesmaFaturaDaCompra() {
            RegraDeFatura regra = new RegraDeFatura(25, 5);
            Fatura daCompra = regra.faturaDaCompra(LocalDate.parse("2026-11-26"));

            assertThat(regra.faturaDaCompetencia(daCompra.competencia())).isEqualTo(daCompra);
        }
    }

    @ParameterizedTest
    @CsvSource({"0, 5", "32, 5", "25, 0", "25, 32"})
    void deveRecusarDiasForaDoIntervalo(int fechamento, int vencimento) {
        assertThatThrownBy(() -> new RegraDeFatura(fechamento, vencimento))
                .isInstanceOfSatisfying(
                        RegraDeNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("DIA_INVALIDO"));
    }
}
