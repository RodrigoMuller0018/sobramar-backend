package io.github.rodrigomuller0018.sobramar.compromisso.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso.Certeza;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso.Forma;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso.Natureza;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeradorDeOcorrenciasTest {

    private static final RegraDeFatura FECHA_25_VENCE_5 = new RegraDeFatura(25, 5);
    private static final YearMonth LONGE = YearMonth.parse("2040-01");

    private static Compromisso compromisso(
            Natureza natureza,
            Forma forma,
            long valor,
            Integer parcelas,
            String inicio,
            String fim,
            boolean anual,
            boolean cartao,
            String faturaInicial,
            String cancelado) {
        return new Compromisso(
                UUID.randomUUID(),
                UUID.randomUUID(),
                natureza,
                forma,
                Certeza.CERTO,
                "Teste",
                valor,
                parcelas,
                LocalDate.parse(inicio),
                fim == null ? null : LocalDate.parse(fim),
                anual,
                cartao ? null : UUID.randomUUID(),
                cartao ? UUID.randomUUID() : null,
                faturaInicial == null ? null : YearMonth.parse(faturaInicial),
                null,
                null,
                null,
                null,
                cancelado == null ? null : LocalDate.parse(cancelado),
                Instant.EPOCH);
    }

    private static Compromisso compraNoCartao(long valor, int parcelas, String data) {
        return compromisso(Natureza.SAIDA, Forma.PARCELADO, valor, parcelas, data, null, false, true, null, null);
    }

    private static Compromisso boleto(long valor, int parcelas, String data) {
        return compromisso(Natureza.SAIDA, Forma.PARCELADO, valor, parcelas, data, null, false, false, null, null);
    }

    private static Compromisso recorrente(long valor, String inicio, String fim) {
        return compromisso(Natureza.SAIDA, Forma.RECORRENTE, valor, null, inicio, fim, false, false, null, null);
    }

    private static List<Ocorrencia> gerar(Compromisso c) {
        return GeradorDeOcorrencias.gerar(c, c.noCartao() ? FECHA_25_VENCE_5 : null, List.of(), LONGE);
    }

    private static List<Ocorrencia> gerar(Compromisso c, AjusteOcorrencia... ajustes) {
        return GeradorDeOcorrencias.gerar(c, c.noCartao() ? FECHA_25_VENCE_5 : null, List.of(ajustes), LONGE);
    }

    @Nested
    class Arredondamento {

        @Test
        void deveColocarADiferencaNaUltimaParcela() {
            assertThat(gerar(boleto(10_000, 3, "2027-01-10")))
                    .extracting(Ocorrencia::valorCentavos)
                    .containsExactly(3333L, 3333L, 3334L);
        }

        @ParameterizedTest(name = "{0} centavos em {1}x")
        @CsvSource({"100, 3", "1, 1", "99999, 7", "480000, 12", "360000, 36", "1000, 120", "7, 3"})
        void aSomaDasParcelasDeveSerSempreOTotal(long total, int parcelas) {
            List<Ocorrencia> ocorrencias = gerar(boleto(total, parcelas, "2027-01-31"));

            assertThat(ocorrencias).hasSize(parcelas);
            assertThat(ocorrencias.stream().mapToLong(Ocorrencia::valorCentavos).sum())
                    .isEqualTo(total);
        }
    }

    @Nested
    class CompraNoCartao {

        @Test
        void cadaParcelaDeveCairEmUmaFaturaSeguinteMantendoADataDaCompra() {
            List<Ocorrencia> ocorrencias = gerar(compraNoCartao(120_000, 12, "2026-11-24"));

            assertThat(ocorrencias).hasSize(12);
            assertThat(ocorrencias.get(0).vencimento()).isEqualTo("2026-12-05");
            assertThat(ocorrencias.get(1).vencimento()).isEqualTo("2027-01-05");
            assertThat(ocorrencias.get(11).vencimento()).isEqualTo("2027-11-05");
            assertThat(ocorrencias).allSatisfy(o -> assertThat(o.dataCompra()).isEqualTo("2026-11-24"));
            assertThat(ocorrencias)
                    .allSatisfy(o -> assertThat(o.valorCentavos()).isEqualTo(10_000));
        }

        @Test
        void compraNoDiaDoFechamentoDeveIrParaAFaturaSeguinte() {
            assertThat(gerar(compraNoCartao(120_000, 12, "2026-11-25"))
                            .getFirst()
                            .competencia())
                    .isEqualTo(YearMonth.parse("2027-01"));
        }

        @Test
        void deveRespeitarAFaturaEscolhidaEContinuarDali() {
            Compromisso c = compromisso(
                    Natureza.SAIDA, Forma.PARCELADO, 30_000, 3, "2026-11-26", null, false, true, "2026-11", null);

            assertThat(gerar(c))
                    .extracting(Ocorrencia::vencimento)
                    .containsExactly(
                            LocalDate.parse("2026-11-05"),
                            LocalDate.parse("2026-12-05"),
                            LocalDate.parse("2027-01-05"));
        }

        @Test
        void assinaturaNoCartaoDeveCairNaFaturaDeCadaCobranca() {
            Compromisso c = compromisso(
                    Natureza.SAIDA, Forma.RECORRENTE, 3_990, null, "2027-01-20", null, false, true, null, null);

            List<Ocorrencia> ocorrencias =
                    GeradorDeOcorrencias.gerar(c, FECHA_25_VENCE_5, List.of(), YearMonth.parse("2027-03"));

            assertThat(ocorrencias)
                    .extracting(Ocorrencia::competencia)
                    .containsExactly(YearMonth.parse("2027-02"), YearMonth.parse("2027-03"));
            assertThat(ocorrencias.get(1).dataCompra()).isEqualTo("2027-02-20");
        }
    }

    @Nested
    class Boleto {

        @Test
        void deveCairNoMesmoDiaDosMesesSeguintesSemTerFatura() {
            List<Ocorrencia> ocorrencias = gerar(boleto(360_000, 36, "2027-02-10"));

            assertThat(ocorrencias).hasSize(36);
            assertThat(ocorrencias.getFirst().vencimento()).isEqualTo("2027-02-10");
            assertThat(ocorrencias.getLast().vencimento()).isEqualTo("2030-01-10");
            assertThat(ocorrencias.getLast().competencia()).isEqualTo(YearMonth.parse("2030-01"));
        }

        @Test
        void diaTrintaEUmDeveVoltarAoDiaOriginalDepoisDeUmMesCurto() {
            assertThat(gerar(boleto(40_000, 4, "2027-01-31")))
                    .extracting(Ocorrencia::vencimento)
                    .containsExactly(
                            LocalDate.parse("2027-01-31"),
                            LocalDate.parse("2027-02-28"),
                            LocalDate.parse("2027-03-31"),
                            LocalDate.parse("2027-04-30"));
        }

        @Test
        void parcelaUnicaDeveTerUmaOcorrenciaSoComOTotal() {
            assertThat(gerar(boleto(5_000, 1, "2027-01-15")))
                    .singleElement()
                    .extracting(Ocorrencia::valorCentavos)
                    .isEqualTo(5_000L);
        }
    }

    @Nested
    class RecorrenteEEvento {

        @Test
        void recorrenteDevePararNoHorizonte() {
            List<Ocorrencia> ocorrencias = GeradorDeOcorrencias.gerar(
                    recorrente(18_000, "2027-01-10", null), null, List.of(), YearMonth.parse("2027-03"));

            assertThat(ocorrencias).hasSize(3);
            assertThat(ocorrencias.getLast().vencimento()).isEqualTo("2027-03-10");
        }

        @Test
        void recorrenteDeveRespeitarADataFinal() {
            assertThat(gerar(recorrente(18_000, "2027-01-10", "2027-02-10"))).hasSize(2);
            assertThat(gerar(recorrente(18_000, "2027-01-10", "2027-02-09"))).hasSize(1);
        }

        @Test
        void eventoDeveAcontecerUmaVezSo() {
            Compromisso c = compromisso(
                    Natureza.ENTRADA, Forma.EVENTO, 500_000, null, "2027-12-20", null, false, false, null, null);

            assertThat(gerar(c)).singleElement().satisfies(o -> {
                assertThat(o.vencimento()).isEqualTo("2027-12-20");
                assertThat(o.valorCentavos()).isEqualTo(500_000);
            });
        }

        @Test
        void eventoAnualDeveRepetirTodoAnoAteOHorizonte() {
            Compromisso c = compromisso(
                    Natureza.SAIDA, Forma.EVENTO, 200_000, null, "2027-03-15", null, true, false, null, null);

            assertThat(GeradorDeOcorrencias.gerar(c, null, List.of(), YearMonth.parse("2029-12")))
                    .extracting(Ocorrencia::vencimento)
                    .containsExactly(
                            LocalDate.parse("2027-03-15"),
                            LocalDate.parse("2028-03-15"),
                            LocalDate.parse("2029-03-15"));
        }
    }

    @Nested
    class CancelamentoEAjustes {

        @Test
        void deveTirarAsOcorrenciasAPartirDoCancelamento() {
            Compromisso c = compromisso(
                    Natureza.SAIDA, Forma.PARCELADO, 30_000, 3, "2027-01-10", null, false, false, null, "2027-02-10");

            assertThat(gerar(c)).hasSize(1);
        }

        @Test
        void somenteEstaNaoDeveMudarAsOutras() {
            List<Ocorrencia> ocorrencias =
                    gerar(boleto(30_000, 3, "2027-01-10"), new AjusteOcorrencia(2, 12_000L, null, null, false, false));

            assertThat(ocorrencias).extracting(Ocorrencia::valorCentavos).containsExactly(10_000L, 12_000L, 10_000L);
        }

        @Test
        void estaEAsFuturasDeveValerDaliEmDiante() {
            List<Ocorrencia> ocorrencias =
                    gerar(boleto(40_000, 4, "2027-01-10"), new AjusteOcorrencia(3, 15_000L, null, null, false, true));

            assertThat(ocorrencias)
                    .extracting(Ocorrencia::valorCentavos)
                    .containsExactly(10_000L, 10_000L, 15_000L, 15_000L);
        }

        @Test
        void ajusteIsoladoDevePrevalecerSobreOPropagado() {
            List<Ocorrencia> ocorrencias = gerar(
                    boleto(40_000, 4, "2027-01-10"),
                    new AjusteOcorrencia(1, 15_000L, null, null, false, true),
                    new AjusteOcorrencia(3, 9_000L, null, null, false, false));

            assertThat(ocorrencias)
                    .extracting(Ocorrencia::valorCentavos)
                    .containsExactly(15_000L, 15_000L, 9_000L, 15_000L);
        }

        @Test
        void deveMudarOVencimentoEACompetenciaDeUmaParcela() {
            Ocorrencia ocorrencia = gerar(
                            boleto(30_000, 3, "2027-01-10"),
                            new AjusteOcorrencia(2, null, LocalDate.parse("2027-03-01"), null, false, false))
                    .get(1);

            assertThat(ocorrencia.vencimento()).isEqualTo("2027-03-01");
            assertThat(ocorrencia.competencia()).isEqualTo(YearMonth.parse("2027-03"));
            assertThat(ocorrencia.valorCentavos()).isEqualTo(10_000);
        }

        @Test
        void deveMudarAFaturaDeUmaParcelaDoCartao() {
            Ocorrencia ocorrencia = gerar(
                            compraNoCartao(30_000, 3, "2026-11-24"),
                            new AjusteOcorrencia(1, null, null, YearMonth.parse("2027-02"), false, false))
                    .getFirst();

            assertThat(ocorrencia.competencia()).isEqualTo(YearMonth.parse("2027-02"));
            assertThat(ocorrencia.vencimento()).isEqualTo("2027-02-05");
        }

        @Test
        void ignorarDeveManterAOcorrenciaMarcada() {
            List<Ocorrencia> ocorrencias =
                    gerar(boleto(20_000, 2, "2027-01-10"), new AjusteOcorrencia(1, null, null, null, true, false));

            assertThat(ocorrencias).extracting(Ocorrencia::ignorada).containsExactly(true, false);
        }
    }
}
