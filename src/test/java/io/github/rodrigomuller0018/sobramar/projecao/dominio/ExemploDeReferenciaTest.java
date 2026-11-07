package io.github.rodrigomuller0018.sobramar.projecao.dominio;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Mes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** A família fictícia de {@code docs/exemplo-de-referencia.md}. Os valores esperados foram calculados à mão. */
class ExemploDeReferenciaTest {

    private static final UUID CONTA = UUID.randomUUID();
    private static final UUID POUPANCA = UUID.randomUUID();
    private static final UUID VALE = UUID.randomUUID();
    private static final UUID CARTAO = UUID.randomUUID();
    private static final UUID ANA = UUID.randomUUID();
    private static final RegraDeFatura REGRA = new RegraDeFatura(25, 5);

    private final List<CompromissoComAjustes> compromissos = new ArrayList<>();
    private final List<Lancamento> lancamentos = new ArrayList<>();
    private Compromisso aluguel;

    private Compromisso compromisso(
            Natureza natureza,
            Forma forma,
            Certeza certeza,
            String descricao,
            long valor,
            Integer parcelas,
            String inicio,
            UUID carteira,
            UUID cartao,
            UUID pessoa) {
        return new Compromisso(
                UUID.randomUUID(),
                UUID.randomUUID(),
                natureza,
                forma,
                certeza,
                descricao,
                valor,
                parcelas,
                LocalDate.parse(inicio),
                null,
                false,
                carteira,
                cartao,
                null,
                pessoa,
                null,
                null,
                null,
                null,
                Instant.EPOCH);
    }

    private Compromisso adicionar(Compromisso c, AjusteOcorrencia... ajustes) {
        compromissos.add(new CompromissoComAjustes(c, List.of(ajustes)));
        return c;
    }

    private Lancamento lancamento(
            Lancamento.Tipo tipo,
            long valor,
            String data,
            UUID carteira,
            UUID destino,
            UUID compromisso,
            Integer numero) {
        return new Lancamento(
                UUID.randomUUID(),
                UUID.randomUUID(),
                tipo,
                "L",
                valor,
                LocalDate.parse(data),
                carteira,
                destino,
                compromisso,
                numero,
                null,
                null,
                false,
                Instant.EPOCH);
    }

    @BeforeEach
    void montarAFamilia() {
        adicionar(compromisso(
                Natureza.ENTRADA,
                Forma.RECORRENTE,
                Certeza.CERTO,
                "Salário",
                500_000,
                null,
                "2027-01-05",
                CONTA,
                null,
                null));
        adicionar(compromisso(
                Natureza.ENTRADA,
                Forma.EVENTO,
                Certeza.CERTO,
                "13º, primeira",
                250_000,
                null,
                "2027-11-20",
                CONTA,
                null,
                null));
        adicionar(compromisso(
                Natureza.ENTRADA,
                Forma.EVENTO,
                Certeza.CERTO,
                "13º, segunda",
                250_000,
                null,
                "2027-12-20",
                CONTA,
                null,
                null));
        adicionar(compromisso(
                Natureza.ENTRADA,
                Forma.RECORRENTE,
                Certeza.CERTO,
                "Vale",
                80_000,
                null,
                "2027-01-01",
                VALE,
                null,
                null));
        aluguel = adicionar(compromisso(
                Natureza.SAIDA,
                Forma.RECORRENTE,
                Certeza.CERTO,
                "Aluguel",
                150_000,
                null,
                "2027-01-10",
                CONTA,
                null,
                null));
        adicionar(
                compromisso(
                        Natureza.SAIDA,
                        Forma.RECORRENTE,
                        Certeza.ESTIMADO,
                        "Luz",
                        18_000,
                        null,
                        "2027-01-15",
                        CONTA,
                        null,
                        null),
                new AjusteOcorrencia(2, null, null, null, true, false));
        adicionar(compromisso(
                Natureza.SAIDA,
                Forma.PARCELADO,
                Certeza.CERTO,
                "Notebook",
                360_000,
                12,
                "2026-12-10",
                null,
                CARTAO,
                ANA));
        adicionar(compromisso(
                Natureza.SAIDA,
                Forma.PARCELADO,
                Certeza.CERTO,
                "Geladeira",
                120_000,
                6,
                "2027-03-25",
                null,
                CARTAO,
                null));
        adicionar(compromisso(
                Natureza.SAIDA,
                Forma.RECORRENTE,
                Certeza.CERTO,
                "Assinatura",
                3_990,
                null,
                "2027-01-20",
                null,
                CARTAO,
                null));
        adicionar(compromisso(
                Natureza.SAIDA, Forma.PARCELADO, Certeza.CERTO, "Curso", 360_000, 36, "2027-02-10", CONTA, null, null));
        adicionar(compromisso(
                Natureza.SAIDA, Forma.EVENTO, Certeza.CERTO, "IPVA", 120_000, null, "2027-03-15", CONTA, null, null));

        lancamentos.add(lancamento(Lancamento.Tipo.GASTO, 155_000, "2027-01-10", CONTA, null, aluguel.id(), 1));
        lancamentos.add(lancamento(Lancamento.Tipo.GASTO, 5_000, "2027-01-12", CONTA, null, null, null));
        lancamentos.add(lancamento(Lancamento.Tipo.TRANSFERENCIA, 20_000, "2027-01-13", CONTA, POUPANCA, null, null));
        lancamentos.add(lancamento(Lancamento.Tipo.GASTO, 3_000, "2027-01-14", VALE, null, null, null));
    }

    private Entrada entrada() {
        return new Entrada(
                compromissos,
                lancamentos,
                Map.of(CARTAO, REGRA),
                Map.of(CARTAO, Set.of(YearMonth.parse("2027-01"))),
                Map.of(CARTAO, Map.of(YearMonth.parse("2027-01"), 30_000L)),
                Set.of(VALE));
    }

    private List<Mes> ano() {
        return CalculadoraDeProjecao.calcular(
                entrada(), YearMonth.parse("2027-01"), YearMonth.parse("2027-12"), Filtro.NENHUM);
    }

    @Test
    void asReceitasDevemBaterComAContaFeitaAMao() {
        assertThat(ano())
                .extracting(Mes::receitasCentavos)
                .containsExactly(
                        500_000L, 500_000L, 500_000L, 500_000L, 500_000L, 500_000L, 500_000L, 500_000L, 500_000L,
                        500_000L, 750_000L, 750_000L);
    }

    @Test
    void osCompromissosDevemBaterComAContaFeitaAMao() {
        assertThat(ano())
                .extracting(Mes::compromissosCentavos)
                .containsExactly(
                        208_000L, 193_990L, 331_990L, 211_990L, 231_990L, 231_990L, 231_990L, 231_990L, 231_990L,
                        231_990L, 211_990L, 211_990L);
    }

    @Test
    void aSobraDeveBaterComAContaFeitaAMao() {
        assertThat(ano())
                .extracting(Mes::sobraCentavos)
                .containsExactly(
                        292_000L, 306_010L, 168_010L, 288_010L, 268_010L, 268_010L, 268_010L, 268_010L, 268_010L,
                        268_010L, 538_010L, 538_010L);
    }

    @Nested
    class OQueNaoPodeEntrarNaSobra {

        @Test
        void valeDeveFicarNumBlocoProprio() {
            List<Mes> ano = ano();

            assertThat(ano.getFirst().vale()).singleElement().satisfies(v -> {
                assertThat(v.carteiraId()).isEqualTo(VALE);
                assertThat(v.creditosCentavos()).isEqualTo(80_000);
                assertThat(v.gastosCentavos()).isEqualTo(3_000);
            });
            assertThat(ano.get(1).vale().getFirst().gastosCentavos()).isZero();
            assertThat(ano.getFirst().itens()).noneMatch(i -> VALE.equals(i.carteiraId()));
        }

        @Test
        void transferenciaNaoDeveAparecer() {
            assertThat(ano().getFirst().itens())
                    .extracting(i -> i.valorCentavos())
                    .doesNotContain(20_000L);
        }

        @Test
        void ignoradoDeveAparecerMasSairDosTotais() {
            Mes fevereiro = ano().get(1);

            assertThat(fevereiro.itens()).anyMatch(i -> "Luz".equals(i.descricao()) && i.ignorada());
            assertThat(fevereiro.contemEstimativa()).isFalse();
        }
    }

    @Nested
    class Detalhes {

        @Test
        void oRealizadoDeveSubstituirOPrevistoSemContarDuasVezes() {
            assertThat(ano().getFirst().itens())
                    .filteredOn(i -> "Aluguel".equals(i.descricao()))
                    .singleElement()
                    .satisfies(i -> {
                        assertThat(i.valorCentavos()).isEqualTo(155_000);
                        assertThat(i.situacao()).isEqualTo(Projecao.Situacao.REALIZADO);
                    });
        }

        @Test
        void aFaturaDeveSomarOCartaoEMarcarSeFoiPaga() {
            List<Mes> ano = ano();

            assertThat(ano.getFirst().faturas()).singleElement().satisfies(f -> {
                assertThat(f.totalCentavos()).isEqualTo(30_000);
                assertThat(f.vencimento()).isEqualTo("2027-01-05");
                assertThat(f.paga()).isTrue();
            });
            assertThat(ano.get(4).faturas().getFirst().totalCentavos()).isEqualTo(53_990);
            assertThat(ano.get(4).faturas().getFirst().paga()).isFalse();
        }

        @Test
        void contaEstimadaDeveMarcarOMes() {
            assertThat(ano().get(2).contemEstimativa()).isTrue();
        }

        @Test
        void oFiltroPorPessoaDeveSomarSoOsItensDela() {
            List<Mes> soDaAna = CalculadoraDeProjecao.calcular(
                    entrada(), YearMonth.parse("2027-03"), YearMonth.parse("2027-03"), new Filtro(ANA, null));

            assertThat(soDaAna).singleElement().satisfies(m -> {
                assertThat(m.compromissosCentavos()).isEqualTo(30_000);
                assertThat(m.receitasCentavos()).isZero();
                assertThat(m.itens()).hasSize(1);
            });
        }

        @Test
        void oFiltroPorCategoriaSemItensDeveZerarOsTotais() {
            List<Mes> nada = CalculadoraDeProjecao.calcular(
                    entrada(),
                    YearMonth.parse("2027-03"),
                    YearMonth.parse("2027-03"),
                    new Filtro(null, UUID.randomUUID()));

            assertThat(nada.getFirst().sobraCentavos()).isZero();
        }

        @Test
        void mesSemNadaDeveVirZeradoEASobraPodeSerNegativa() {
            compromissos.clear();
            lancamentos.clear();
            adicionar(compromisso(
                    Natureza.SAIDA,
                    Forma.EVENTO,
                    Certeza.CERTO,
                    "IPVA",
                    120_000,
                    null,
                    "2027-03-15",
                    CONTA,
                    null,
                    null));

            List<Mes> meses = CalculadoraDeProjecao.calcular(
                    entrada(), YearMonth.parse("2027-02"), YearMonth.parse("2027-03"), Filtro.NENHUM);

            assertThat(meses.getFirst().sobraCentavos()).isZero();
            assertThat(meses.get(1).sobraCentavos()).isEqualTo(-120_000);
        }
    }
}
