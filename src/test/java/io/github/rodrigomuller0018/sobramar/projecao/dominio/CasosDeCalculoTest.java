package io.github.rodrigomuller0018.sobramar.projecao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.AjusteOcorrencia;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso.Certeza;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso.Forma;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso.Natureza;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.Lancamento;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDeProjecao.CompromissoComAjustes;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDeProjecao.Entrada;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDeProjecao.Filtro;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Confianca;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Item;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Mes;
import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Os casos que protegem a sobra (Fase 0 da concepção): fim de mês, centavo de diferença, mês de 28 dias, limites
 * de parcelas, conciliação da fatura, parte por pessoa e confiança do número. Tudo em centavos, pela calculadora
 * inteira. A família completa fica em {@link ExemploDeReferenciaTest}.
 */
class CasosDeCalculoTest {

    private static final UUID CONTA = UUID.randomUUID();
    private static final UUID VALE = UUID.randomUUID();
    private static final UUID CARTAO = UUID.randomUUID();
    private static final RegraDeFatura FECHA_25_VENCE_5 = new RegraDeFatura(25, 5);

    private final List<CompromissoComAjustes> compromissos = new ArrayList<>();
    private final List<Lancamento> lancamentos = new ArrayList<>();
    private RegraDeFatura regra = FECHA_25_VENCE_5;
    private Map<UUID, Set<YearMonth>> faturasPagas = Map.of();
    private Map<UUID, Map<YearMonth, Long>> faturasInformadas = Map.of();

    private static Compromisso compromisso(
            Natureza natureza,
            Forma forma,
            Certeza certeza,
            long valor,
            Integer parcelas,
            String inicio,
            UUID carteira,
            UUID cartao,
            Integer parte,
            boolean anual,
            String canceladoAPartirDe) {
        return new Compromisso(
                UUID.randomUUID(),
                CONTA,
                natureza,
                forma,
                certeza,
                "Teste",
                valor,
                parcelas,
                LocalDate.parse(inicio),
                null,
                anual,
                carteira,
                cartao,
                null,
                null,
                null,
                parte,
                canceladoAPartirDe == null ? null : LocalDate.parse(canceladoAPartirDe),
                Instant.EPOCH);
    }

    private Compromisso boleto(long total, int parcelas, String inicio) {
        return adicionar(compromisso(
                Natureza.SAIDA,
                Forma.PARCELADO,
                Certeza.CERTO,
                total,
                parcelas,
                inicio,
                CONTA,
                null,
                null,
                false,
                null));
    }

    private Compromisso compraNoCartao(long total, int parcelas, String data) {
        return adicionar(compromisso(
                Natureza.SAIDA,
                Forma.PARCELADO,
                Certeza.CERTO,
                total,
                parcelas,
                data,
                null,
                CARTAO,
                null,
                false,
                null));
    }

    private Compromisso recorrente(Natureza natureza, Certeza certeza, long valor, String inicio, Integer parte) {
        return adicionar(
                compromisso(natureza, Forma.RECORRENTE, certeza, valor, null, inicio, CONTA, null, parte, false, null));
    }

    private Compromisso evento(Natureza natureza, long valor, String data, boolean anual) {
        return adicionar(
                compromisso(natureza, Forma.EVENTO, Certeza.CERTO, valor, null, data, CONTA, null, null, anual, null));
    }

    private Compromisso adicionar(Compromisso c, AjusteOcorrencia... ajustes) {
        compromissos.add(new CompromissoComAjustes(c, List.of(ajustes)));
        return c;
    }

    private void pagar(Compromisso c, int numero, long valor, String data) {
        lancamentos.add(lancamento(Lancamento.Tipo.GASTO, valor, data, CONTA, null, c.id(), numero));
    }

    private static Lancamento lancamento(
            Lancamento.Tipo tipo, long valor, String data, UUID carteira, UUID destino, UUID compromisso, Integer n) {
        return new Lancamento(
                UUID.randomUUID(),
                CONTA,
                tipo,
                "L",
                valor,
                LocalDate.parse(data),
                carteira,
                destino,
                compromisso,
                n,
                null,
                null,
                false,
                Instant.EPOCH);
    }

    private Entrada entrada() {
        return new Entrada(
                compromissos, lancamentos, Map.of(CARTAO, regra), faturasPagas, faturasInformadas, Set.of(VALE));
    }

    private List<Mes> meses(String de, String ate) {
        return CalculadoraDeProjecao.calcular(entrada(), YearMonth.parse(de), YearMonth.parse(ate), Filtro.NENHUM);
    }

    private Mes mes(String competencia) {
        return meses(competencia, competencia).getFirst();
    }

    private static long somaDasSobras(List<Mes> meses) {
        return meses.stream().mapToLong(Mes::sobraCentavos).sum();
    }

    private static List<Long> valores(Mes mes) {
        return mes.itens().stream().map(Item::valorCentavos).toList();
    }

    @Nested
    class Parcelas {

        @Test
        void caso01_centavoDeDiferencaVaiNaUltimaParcela() {
            boleto(10_000, 3, "2027-01-10");

            List<Mes> ano = meses("2027-01", "2027-03");

            assertThat(ano).extracting(Mes::compromissosCentavos).containsExactly(3_333L, 3_333L, 3_334L);
            assertThat(somaDasSobras(ano)).isEqualTo(-10_000);
        }

        @Test
        void caso02_umCentavoEmDuasParcelasNaoFicaNegativoESomaUmCentavo() {
            boleto(1, 2, "2027-01-10");

            List<Mes> meses = meses("2027-01", "2027-02");

            assertThat(meses).extracting(Mes::compromissosCentavos).allMatch(v -> v >= 0);
            assertThat(somaDasSobras(meses)).isEqualTo(-1);
        }

        @Test
        void caso03_quarentaEOitoParcelasNoCartaoTerminamQuatroAnosDepois() {
            compraNoCartao(480_000, 48, "2027-01-10");

            List<Mes> tudo = meses("2027-01", "2031-03");

            assertThat(mes("2027-01").compromissosCentavos()).isZero();
            assertThat(mes("2027-02").compromissosCentavos()).isEqualTo(10_000);
            assertThat(mes("2031-01").compromissosCentavos()).isEqualTo(10_000);
            assertThat(mes("2031-02").compromissosCentavos()).isZero();
            assertThat(somaDasSobras(tudo)).isEqualTo(-480_000);
        }

        @Test
        void caso04_centoEVinteParcelasNoBoletoSomamOTotalExato() {
            boleto(100_000, 120, "2027-01-10");

            List<Mes> tudo = meses("2027-01", "2036-12");

            assertThat(mes("2027-01").compromissosCentavos()).isEqualTo(833);
            assertThat(mes("2036-12").compromissosCentavos()).isEqualTo(873);
            assertThat(somaDasSobras(tudo)).isEqualTo(-100_000);
        }

        @Test
        void caso05_trintaESeisParcelasFechamOAnoComOnzeEContinuamDepois() {
            boleto(360_000, 36, "2027-02-10");

            assertThat(somaDasSobras(meses("2027-01", "2027-12"))).isEqualTo(-110_000);
            assertThat(mes("2030-01").compromissosCentavos()).isEqualTo(10_000);
            assertThat(mes("2030-02").compromissosCentavos()).isZero();
        }
    }

    @Nested
    class FimDeMes {

        @Test
        void caso06_boletoDia31CaiNoUltimoDiaDeFevereiroEVoltaAoDia31() {
            boleto(30_000, 3, "2027-01-31");

            assertThat(mes("2027-02").itens().getFirst().vencimento()).isEqualTo("2027-02-28");
            assertThat(mes("2027-03").itens().getFirst().vencimento()).isEqualTo("2027-03-31");
        }

        @Test
        void caso07_recorrenteDia30EmFevereiroDe28DiasCaiNoDia28() {
            recorrente(Natureza.SAIDA, Certeza.CERTO, 5_000, "2027-01-30", null);

            assertThat(mes("2027-02").itens().getFirst().vencimento()).isEqualTo("2027-02-28");
        }

        @Test
        void caso08_compraEmAnoBissextoMantemADataDaCompraEmTodasAsParcelas() {
            boleto(30_000, 3, "2028-02-29");

            List<Mes> meses = meses("2028-02", "2028-04");

            assertThat(meses)
                    .allSatisfy(
                            m -> assertThat(m.itens().getFirst().dataCompra()).isEqualTo("2028-02-29"));
            assertThat(meses.get(2).itens().getFirst().vencimento()).isEqualTo("2028-04-29");
        }

        @Test
        void caso09_cartaoQueFechaDia31EmAbrilFechaNoDia30() {
            regra = new RegraDeFatura(31, 10);
            compraNoCartao(10_000, 1, "2027-04-30");
            compraNoCartao(20_000, 1, "2027-04-29");

            assertThat(mes("2027-05").compromissosCentavos()).isEqualTo(20_000);
            assertThat(mes("2027-06").compromissosCentavos()).isEqualTo(10_000);
        }
    }

    @Nested
    class Fatura {

        @Test
        void caso10_compraNaVesperaDoFechamentoCaiNaFaturaAtualENoDiaCaiNaSeguinte() {
            compraNoCartao(10_000, 1, "2027-01-24");
            compraNoCartao(20_000, 1, "2027-01-25");

            assertThat(mes("2027-02").compromissosCentavos()).isEqualTo(10_000);
            assertThat(mes("2027-03").compromissosCentavos()).isEqualTo(20_000);
        }

        @Test
        void caso11_cartaoQueVenceDepoisDoFechamentoVenceNoMesmoMes() {
            regra = new RegraDeFatura(5, 15);
            compraNoCartao(10_000, 1, "2027-03-01");

            Mes marco = mes("2027-03");

            assertThat(marco.compromissosCentavos()).isEqualTo(10_000);
            assertThat(marco.faturas().getFirst().vencimento()).isEqualTo("2027-03-15");
        }

        @Test
        void caso12_pagarAFaturaMarcaAsComprasComoRealizadasSemGastoNovo() {
            compraNoCartao(30_000, 1, "2027-01-10");
            faturasPagas = Map.of(CARTAO, Set.of(YearMonth.parse("2027-02")));
            faturasInformadas = Map.of(CARTAO, Map.of(YearMonth.parse("2027-02"), 30_000L));

            Mes fevereiro = mes("2027-02");

            assertThat(fevereiro.compromissosCentavos()).isEqualTo(30_000);
            assertThat(fevereiro.itens().getFirst().situacao()).isEqualTo(Projecao.Situacao.REALIZADO);
            assertThat(fevereiro.faturas().getFirst().paga()).isTrue();
        }

        @Test
        void caso13_faturaSemTotalInformadoDeixaOMesEstimado() {
            compraNoCartao(30_000, 1, "2027-01-10");

            Mes fevereiro = mes("2027-02");

            assertThat(fevereiro.confianca()).isEqualTo(Confianca.ESTIMADA);
            assertThat(fevereiro.faturas().getFirst().conciliada()).isFalse();
            assertThat(fevereiro.faturas().getFirst().diferencaCentavos()).isNull();
        }

        @Test
        void caso14_faturaInformadaIgualAoCalculadoDeixaOMesFirme() {
            compraNoCartao(30_000, 1, "2027-01-10");
            faturasInformadas = Map.of(CARTAO, Map.of(YearMonth.parse("2027-02"), 30_000L));

            Mes fevereiro = mes("2027-02");

            assertThat(fevereiro.confianca()).isEqualTo(Confianca.FIRME);
            assertThat(fevereiro.faturas().getFirst().conciliada()).isTrue();
            assertThat(fevereiro.faturas().getFirst().diferencaCentavos()).isZero();
        }

        @Test
        void caso15_faturaInformadaDiferenteMostraQuantoFaltaExplicar() {
            compraNoCartao(30_000, 1, "2027-01-10");
            faturasInformadas = Map.of(CARTAO, Map.of(YearMonth.parse("2027-02"), 35_000L));

            Mes fevereiro = mes("2027-02");

            assertThat(fevereiro.confianca()).isEqualTo(Confianca.ESTIMADA);
            assertThat(fevereiro.faturas().getFirst().informadoCentavos()).isEqualTo(35_000);
            assertThat(fevereiro.faturas().getFirst().diferencaCentavos()).isEqualTo(5_000);
            assertThat(fevereiro.compromissosCentavos()).isEqualTo(30_000);
        }
    }

    @Nested
    class Confiabilidade {

        @Test
        void caso16_estimadoPrevistoDeixaOMesEstimadoERealizadoDeixaFirme() {
            Compromisso luz = recorrente(Natureza.SAIDA, Certeza.ESTIMADO, 18_000, "2027-01-15", null);

            assertThat(mes("2027-01").confianca()).isEqualTo(Confianca.ESTIMADA);

            pagar(luz, 1, 17_250, "2027-01-15");

            Mes janeiro = mes("2027-01");
            assertThat(janeiro.confianca()).isEqualTo(Confianca.FIRME);
            assertThat(janeiro.compromissosCentavos()).isEqualTo(17_250);
        }

        @Test
        void caso17_mesSemItemEstimadoESemFaturaEhFirme() {
            recorrente(Natureza.ENTRADA, Certeza.CERTO, 500_000, "2027-01-05", null);
            boleto(10_000, 1, "2027-01-10");

            assertThat(mes("2027-01").confianca()).isEqualTo(Confianca.FIRME);
        }

        @Test
        void caso18_pagamentoAdiantadoContaNoMesDoPagamentoComOValorReal() {
            Compromisso curso = boleto(20_000, 2, "2027-01-10");
            pagar(curso, 2, 9_900, "2027-01-20");

            List<Mes> meses = meses("2027-01", "2027-02");

            assertThat(meses.getFirst().compromissosCentavos()).isEqualTo(19_900);
            assertThat(meses.get(1).compromissosCentavos()).isZero();
        }

        @Test
        void caso19_atualizadoEmEhOInstanteMaisRecenteOuNadaQuandoVazio() {
            Instant antigo = Instant.parse("2027-01-01T10:00:00Z");
            Instant recente = Instant.parse("2027-03-15T12:30:00Z");

            assertThat(Projecao.ultimaAtualizacao(List.of(antigo, recente, antigo)))
                    .contains(recente);
            assertThat(Projecao.ultimaAtualizacao(List.of())).isEmpty();
        }
    }

    @Nested
    class Parte {

        @Test
        void caso20_metadeDoAluguelArredondaMeioCentavoParaCima() {
            recorrente(Natureza.SAIDA, Certeza.CERTO, 150_001, "2027-01-10", 50);

            assertThat(mes("2027-01").compromissosCentavos()).isEqualTo(75_001);
        }

        @Test
        void caso21_parteEmParceladoAplicaNoTotalAntesDeDividir() {
            adicionar(compromisso(
                    Natureza.SAIDA,
                    Forma.PARCELADO,
                    Certeza.CERTO,
                    10_000,
                    3,
                    "2027-01-10",
                    CONTA,
                    null,
                    33,
                    false,
                    null));

            List<Mes> meses = meses("2027-01", "2027-03");

            assertThat(meses).extracting(Mes::compromissosCentavos).containsExactly(1_100L, 1_100L, 1_100L);
        }

        @Test
        void caso22_parteForaDeUmACemEhRecusada() {
            assertThatThrownBy(() -> compromisso(
                            Natureza.SAIDA,
                            Forma.RECORRENTE,
                            Certeza.CERTO,
                            1_000,
                            null,
                            "2027-01-10",
                            CONTA,
                            null,
                            0,
                            false,
                            null))
                    .isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> compromisso(
                            Natureza.SAIDA,
                            Forma.RECORRENTE,
                            Certeza.CERTO,
                            1_000,
                            null,
                            "2027-01-10",
                            CONTA,
                            null,
                            101,
                            false,
                            null))
                    .isInstanceOf(RegraDeNegocioException.class);
        }
    }

    @Nested
    class OQueEntraEOQueNao {

        @Test
        void caso23_transferenciaNaoEntraEReceitaAvulsaEntra() {
            lancamentos.add(lancamento(
                    Lancamento.Tipo.TRANSFERENCIA, 20_000, "2027-01-13", CONTA, UUID.randomUUID(), null, null));
            lancamentos.add(lancamento(Lancamento.Tipo.RECEITA, 1_000, "2027-01-14", CONTA, null, null, null));

            Mes janeiro = mes("2027-01");

            assertThat(janeiro.receitasCentavos()).isEqualTo(1_000);
            assertThat(janeiro.compromissosCentavos()).isZero();
            assertThat(valores(janeiro)).doesNotContain(20_000L);
        }

        @Test
        void caso24_ignoradaApareceMasSaiDosTotais() {
            adicionar(
                    compromisso(
                            Natureza.SAIDA,
                            Forma.RECORRENTE,
                            Certeza.CERTO,
                            5_000,
                            null,
                            "2027-01-10",
                            CONTA,
                            null,
                            null,
                            false,
                            null),
                    new AjusteOcorrencia(1, null, null, null, true, false));

            Mes janeiro = mes("2027-01");

            assertThat(janeiro.itens()).singleElement().matches(Item::ignorada);
            assertThat(janeiro.compromissosCentavos()).isZero();
        }

        @Test
        void caso25_valeFicaForaDaSobra() {
            adicionar(compromisso(
                    Natureza.ENTRADA,
                    Forma.RECORRENTE,
                    Certeza.CERTO,
                    80_000,
                    null,
                    "2027-01-01",
                    VALE,
                    null,
                    null,
                    false,
                    null));

            Mes janeiro = mes("2027-01");

            assertThat(janeiro.sobraCentavos()).isZero();
            assertThat(janeiro.vale().getFirst().creditosCentavos()).isEqualTo(80_000);
        }

        @Test
        void caso26_canceladoAPartirDeTiraAsParcelasRestantesESobraNegativaEhPermitida() {
            adicionar(compromisso(
                    Natureza.SAIDA,
                    Forma.PARCELADO,
                    Certeza.CERTO,
                    30_000,
                    3,
                    "2027-01-10",
                    CONTA,
                    null,
                    null,
                    false,
                    "2027-03-01"));

            List<Mes> meses = meses("2027-01", "2027-03");

            assertThat(meses).extracting(Mes::sobraCentavos).containsExactly(-10_000L, -10_000L, 0L);
        }

        @Test
        void caso27_eventoAnualRepeteNoAnoSeguinteEDecimoTerceiroCaiNosMesesCertos() {
            evento(Natureza.SAIDA, 120_000, "2027-03-15", true);
            evento(Natureza.ENTRADA, 250_000, "2027-11-20", false);
            evento(Natureza.ENTRADA, 250_000, "2027-12-20", false);

            assertThat(mes("2028-03").compromissosCentavos()).isEqualTo(120_000);
            assertThat(mes("2027-11").receitasCentavos()).isEqualTo(250_000);
            assertThat(mes("2027-12").receitasCentavos()).isEqualTo(250_000);
            assertThat(mes("2028-11").receitasCentavos()).isZero();
        }
    }
}
