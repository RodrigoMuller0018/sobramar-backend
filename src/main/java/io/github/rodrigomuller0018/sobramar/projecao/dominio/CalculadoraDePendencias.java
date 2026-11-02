package io.github.rodrigomuller0018.sobramar.projecao.dominio;

import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.GeradorDeOcorrencias;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Ocorrencia;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.Lancamento;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDeProjecao.CompromissoComAjustes;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDeProjecao.Entrada;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** O que precisa de atenção: saídas não pagas, vencidas ou perto de vencer. Mesmas regras da projeção. */
public final class CalculadoraDePendencias {

    private CalculadoraDePendencias() {}

    public enum Tipo {
        FATURA,
        OCORRENCIA
    }

    public record Pendencia(
            Tipo tipo,
            String descricao,
            LocalDate vencimento,
            long diasAteVencimento,
            long valorCentavos,
            UUID cartaoId,
            YearMonth competencia,
            UUID compromissoId,
            Integer numeroOcorrencia) {}

    private record ChaveDeFatura(UUID cartaoId, YearMonth competencia) {}

    public static List<Pendencia> calcular(
            Entrada entrada, LocalDate hoje, int dias, Map<UUID, String> nomesDosCartoes) {
        LocalDate limite = hoje.plusDays(dias);
        Set<String> pagas = entrada.lancamentos().stream()
                .filter(l -> l.compromissoId() != null)
                .map(CalculadoraDePendencias::chaveDoPagamento)
                .collect(Collectors.toSet());
        List<Pendencia> pendencias = new ArrayList<>();
        Map<ChaveDeFatura, Long> faturas = new HashMap<>();
        for (CompromissoComAjustes c : entrada.compromissos()) {
            if (conta(c.compromisso(), entrada)) {
                for (Ocorrencia o : ocorrencias(c, entrada, limite)) {
                    registrar(c.compromisso(), o, hoje, limite, pagas, pendencias, faturas);
                }
            }
        }
        faturas.forEach(
                (chave, total) -> adicionarFatura(chave, total, entrada, hoje, limite, nomesDosCartoes, pendencias));
        pendencias.sort(Comparator.comparing(Pendencia::vencimento));
        return pendencias;
    }

    /** Entradas não pedem ação, e o vale não sai da conta. */
    private static boolean conta(Compromisso c, Entrada entrada) {
        return c.natureza() == Compromisso.Natureza.SAIDA && !CalculadoraDeProjecao.ehVale(entrada, c.carteiraId());
    }

    private static List<Ocorrencia> ocorrencias(CompromissoComAjustes c, Entrada entrada, LocalDate limite) {
        RegraDeFatura regra = c.compromisso().noCartao()
                ? entrada.regrasDosCartoes().get(c.compromisso().cartaoId())
                : null;
        return GeradorDeOcorrencias.gerar(c.compromisso(), regra, c.ajustes(), YearMonth.from(limite));
    }

    private static void registrar(
            Compromisso c,
            Ocorrencia o,
            LocalDate hoje,
            LocalDate limite,
            Set<String> pagas,
            List<Pendencia> pendencias,
            Map<ChaveDeFatura, Long> faturas) {
        if (o.ignorada()) {
            return;
        }
        if (c.noCartao()) {
            faturas.merge(new ChaveDeFatura(c.cartaoId(), o.competencia()), o.valorCentavos(), Long::sum);
        } else if (!pagas.contains(chave(c.id(), o.numero())) && !o.vencimento().isAfter(limite)) {
            pendencias.add(new Pendencia(
                    Tipo.OCORRENCIA,
                    c.descricao(),
                    o.vencimento(),
                    ChronoUnit.DAYS.between(hoje, o.vencimento()),
                    o.valorCentavos(),
                    null,
                    null,
                    c.id(),
                    o.numero()));
        }
    }

    private static void adicionarFatura(
            ChaveDeFatura chave,
            long total,
            Entrada entrada,
            LocalDate hoje,
            LocalDate limite,
            Map<UUID, String> nomes,
            List<Pendencia> pendencias) {
        boolean paga =
                entrada.faturasPagas().getOrDefault(chave.cartaoId(), Set.of()).contains(chave.competencia());
        LocalDate vencimento = entrada.regrasDosCartoes()
                .get(chave.cartaoId())
                .faturaDaCompetencia(chave.competencia())
                .vencimento();
        if (!paga && !vencimento.isAfter(limite)) {
            pendencias.add(new Pendencia(
                    Tipo.FATURA,
                    nomes.get(chave.cartaoId()),
                    vencimento,
                    ChronoUnit.DAYS.between(hoje, vencimento),
                    total,
                    chave.cartaoId(),
                    chave.competencia(),
                    null,
                    null));
        }
    }

    private static String chaveDoPagamento(Lancamento l) {
        return chave(l.compromissoId(), l.numeroOcorrencia());
    }

    private static String chave(UUID compromissoId, int numero) {
        return compromissoId + "#" + numero;
    }
}
