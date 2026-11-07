package io.github.rodrigomuller0018.sobramar.projecao.dominio;

import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.AjusteOcorrencia;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.GeradorDeOcorrencias;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Ocorrencia;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.Lancamento;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Confianca;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Fatura;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Item;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Mes;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Situacao;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao.Vale;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * O número central do produto (ADR 0002, decisões 2, 5 e 7). Função pura: a linha do tempo, a simulação e o
 * exemplo de referência usam exatamente a mesma conta. Confira {@code docs/exemplo-de-referencia.md}.
 */
public final class CalculadoraDeProjecao {

    private CalculadoraDeProjecao() {}

    public record CompromissoComAjustes(Compromisso compromisso, List<AjusteOcorrencia> ajustes) {}

    /**
     * @param faturasInformadas por cartão e competência, o total que o banco cobrou (informado ou pago). É com ele
     *     que a fatura calculada se concilia.
     */
    public record Entrada(
            List<CompromissoComAjustes> compromissos,
            List<Lancamento> lancamentos,
            Map<UUID, RegraDeFatura> regrasDosCartoes,
            Map<UUID, Set<YearMonth>> faturasPagas,
            Map<UUID, Map<YearMonth, Long>> faturasInformadas,
            Set<UUID> carteirasDeVale) {}

    public record Filtro(UUID pessoaId, UUID categoriaId) {

        public static final Filtro NENHUM = new Filtro(null, null);

        boolean aceita(UUID pessoa, UUID categoria) {
            return (pessoaId == null || pessoaId.equals(pessoa))
                    && (categoriaId == null || categoriaId.equals(categoria));
        }
    }

    /** Um item ainda sem separar por tipo, já no mês em que o dinheiro se move. */
    private record Movimento(YearMonth mes, Item item) {}

    public static List<Mes> calcular(Entrada entrada, YearMonth de, YearMonth ate, Filtro filtro) {
        List<Movimento> movimentos = new ArrayList<>();
        Map<UUID, Map<Integer, Lancamento>> pagamentos = pagamentosPorCompromisso(entrada.lancamentos());
        for (CompromissoComAjustes c : entrada.compromissos()) {
            if (filtro.aceita(c.compromisso().pessoaId(), c.compromisso().categoriaId())) {
                movimentos.addAll(movimentosDoCompromisso(c, entrada, pagamentos, ate));
            }
        }
        for (Lancamento avulso : entrada.lancamentos()) {
            if (ehAvulsoQueConta(avulso) && filtro.aceita(avulso.pessoaId(), avulso.categoriaId())) {
                movimentos.add(movimentoDoAvulso(avulso));
            }
        }
        Map<YearMonth, List<Item>> porMes = movimentos.stream()
                .collect(Collectors.groupingBy(
                        Movimento::mes, TreeMap::new, Collectors.mapping(Movimento::item, Collectors.toList())));
        List<Mes> meses = new ArrayList<>();
        for (YearMonth mes = de; !mes.isAfter(ate); mes = mes.plusMonths(1)) {
            meses.add(mes(mes, porMes.getOrDefault(mes, List.of()), entrada, filtro));
        }
        return meses;
    }

    private static Map<UUID, Map<Integer, Lancamento>> pagamentosPorCompromisso(List<Lancamento> lancamentos) {
        return lancamentos.stream()
                .filter(l -> l.compromissoId() != null)
                .collect(Collectors.groupingBy(
                        Lancamento::compromissoId, Collectors.toMap(Lancamento::numeroOcorrencia, l -> l)));
    }

    private static List<Movimento> movimentosDoCompromisso(
            CompromissoComAjustes c, Entrada entrada, Map<UUID, Map<Integer, Lancamento>> pagamentos, YearMonth ate) {
        Compromisso compromisso = c.compromisso();
        RegraDeFatura regra =
                compromisso.noCartao() ? entrada.regrasDosCartoes().get(compromisso.cartaoId()) : null;
        Map<Integer, Lancamento> pagas = pagamentos.getOrDefault(compromisso.id(), Map.of());
        Set<YearMonth> faturasPagas = compromisso.noCartao()
                ? entrada.faturasPagas().getOrDefault(compromisso.cartaoId(), Set.of())
                : Set.of();
        return GeradorDeOcorrencias.gerar(compromisso, regra, c.ajustes(), ate).stream()
                .map(o -> movimento(compromisso, o, pagas.get(o.numero()), faturasPagas))
                .toList();
    }

    private static Movimento movimento(Compromisso c, Ocorrencia o, Lancamento pagamento, Set<YearMonth> faturasPagas) {
        if (pagamento != null) {
            return new Movimento(
                    YearMonth.from(pagamento.data()),
                    item(c, o, pagamento.valorCentavos(), o.ignorada() || pagamento.ignorado(), Situacao.REALIZADO));
        }
        Situacao situacao = faturasPagas.contains(o.competencia()) ? Situacao.REALIZADO : Situacao.PREVISTO;
        return new Movimento(o.competencia(), item(c, o, o.valorCentavos(), o.ignorada(), situacao));
    }

    private static Item item(Compromisso c, Ocorrencia o, long valor, boolean ignorada, Situacao situacao) {
        return new Item(
                c.id(),
                o.numero(),
                c.parcelas(),
                c.descricao(),
                c.natureza(),
                c.certeza(),
                valor,
                o.dataCompra(),
                o.vencimento(),
                ignorada,
                c.cartaoId(),
                c.carteiraId(),
                c.pessoaId(),
                c.categoriaId(),
                situacao);
    }

    /** Transferência só muda o dinheiro de lugar, e pagamento de parcela já entrou como realizado. */
    private static boolean ehAvulsoQueConta(Lancamento l) {
        return l.compromissoId() == null && l.tipo() != Lancamento.Tipo.TRANSFERENCIA;
    }

    private static Movimento movimentoDoAvulso(Lancamento l) {
        Compromisso.Natureza natureza =
                l.tipo() == Lancamento.Tipo.RECEITA ? Compromisso.Natureza.ENTRADA : Compromisso.Natureza.SAIDA;
        Item item = new Item(
                null,
                null,
                null,
                l.descricao(),
                natureza,
                Compromisso.Certeza.CERTO,
                l.valorCentavos(),
                l.data(),
                l.data(),
                l.ignorado(),
                null,
                l.carteiraId(),
                l.pessoaId(),
                l.categoriaId(),
                Situacao.REALIZADO);
        return new Movimento(YearMonth.from(l.data()), item);
    }

    private static Mes mes(YearMonth competencia, List<Item> itens, Entrada entrada, Filtro filtro) {
        List<Item> fora =
                itens.stream().filter(i -> ehVale(entrada, i.carteiraId())).toList();
        List<Item> daConta = itens.stream()
                .filter(i -> !ehVale(entrada, i.carteiraId()))
                .sorted(Comparator.comparing(Item::vencimento))
                .toList();
        List<Item> contados = daConta.stream().filter(i -> !i.ignorada()).toList();
        boolean contemEstimativa = contados.stream()
                .anyMatch(i -> i.certeza() == Compromisso.Certeza.ESTIMADO && i.situacao() == Situacao.PREVISTO);
        List<Fatura> faturas = faturas(competencia, contados, entrada, filtro);
        boolean conciliado = faturas.stream().allMatch(Fatura::conciliada);
        return new Mes(
                competencia,
                soma(contados, Compromisso.Natureza.ENTRADA),
                soma(contados, Compromisso.Natureza.SAIDA),
                contemEstimativa,
                contemEstimativa || !conciliado ? Confianca.ESTIMADA : Confianca.FIRME,
                faturas,
                daConta,
                vale(fora));
    }

    /** Compra no cartão não tem carteira. */
    static boolean ehVale(Entrada entrada, UUID carteiraId) {
        return carteiraId != null && entrada.carteirasDeVale().contains(carteiraId);
    }

    private static long soma(List<Item> itens, Compromisso.Natureza natureza) {
        return itens.stream()
                .filter(i -> i.natureza() == natureza)
                .mapToLong(Item::valorCentavos)
                .sum();
    }

    /** Com filtro, a fatura é parcial e não se concilia com o total do banco. */
    private static List<Fatura> faturas(YearMonth competencia, List<Item> contados, Entrada entrada, Filtro filtro) {
        Map<UUID, Long> totais = contados.stream()
                .filter(i -> i.cartaoId() != null)
                .collect(Collectors.groupingBy(
                        Item::cartaoId, TreeMap::new, Collectors.summingLong(Item::valorCentavos)));
        boolean completa = Filtro.NENHUM.equals(filtro);
        return totais.entrySet().stream()
                .map(total -> new Fatura(
                        total.getKey(),
                        entrada.regrasDosCartoes()
                                .get(total.getKey())
                                .faturaDaCompetencia(competencia)
                                .vencimento(),
                        total.getValue(),
                        entrada.faturasPagas()
                                .getOrDefault(total.getKey(), Set.of())
                                .contains(competencia),
                        completa
                                ? entrada.faturasInformadas()
                                        .getOrDefault(total.getKey(), Map.of())
                                        .get(competencia)
                                : null))
                .toList();
    }

    private static List<Vale> vale(List<Item> itens) {
        return itens.stream()
                .filter(i -> !i.ignorada())
                .collect(Collectors.groupingBy(Item::carteiraId, TreeMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(e -> new Vale(
                        e.getKey(),
                        soma(e.getValue(), Compromisso.Natureza.ENTRADA),
                        soma(e.getValue(), Compromisso.Natureza.SAIDA)))
                .toList();
    }
}
