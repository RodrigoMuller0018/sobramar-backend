package io.github.rodrigomuller0018.sobramar.projecao.dominio;

import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** O resultado da conta: a linha do tempo, mês a mês. */
public final class Projecao {

    private Projecao() {}

    public enum Situacao {
        PREVISTO,
        REALIZADO
    }

    /**
     * Quanto dá para confiar na sobra do mês. ESTIMADA enquanto houver valor estimado ainda previsto ou fatura que
     * não bate com o total do banco; FIRME só quando tudo é certo e conciliado. O front mostra a lápis ou em tinta.
     */
    public enum Confianca {
        FIRME,
        ESTIMADA
    }

    public record Item(
            UUID compromissoId,
            Integer numeroOcorrencia,
            Integer totalOcorrencias,
            String descricao,
            Compromisso.Natureza natureza,
            Compromisso.Certeza certeza,
            long valorCentavos,
            /** Em estimativa com faixa, até quanto pode chegar; nulo nos demais. */
            Long valorMaximoCentavos,
            LocalDate dataCompra,
            LocalDate vencimento,
            boolean ignorada,
            UUID cartaoId,
            UUID carteiraId,
            UUID pessoaId,
            UUID categoriaId,
            Situacao situacao) {}

    /**
     * @param informadoCentavos o total que o banco cobrou, quando a pessoa informou ou pagou a fatura; nulo se não
     */
    public record Fatura(
            UUID cartaoId, LocalDate vencimento, long totalCentavos, boolean paga, Long informadoCentavos) {

        /** O que "falta explicar": informado menos calculado. Nulo sem total informado. */
        public Long diferencaCentavos() {
            return informadoCentavos == null ? null : informadoCentavos - totalCentavos;
        }

        public boolean conciliada() {
            return informadoCentavos != null && informadoCentavos == totalCentavos;
        }
    }

    public record Vale(UUID carteiraId, long creditosCentavos, long gastosCentavos) {}

    public record Mes(
            YearMonth competencia,
            long receitasCentavos,
            long compromissosCentavos,
            boolean contemEstimativa,
            Confianca confianca,
            /** O que as entradas estimadas com faixa podem render além do mínimo, enquanto previstas. */
            long extraPossivelCentavos,
            List<Fatura> faturas,
            List<Item> itens,
            List<Vale> vale) {

        /** Pode ser negativa. Conta com o mínimo das estimativas: o incerto nunca entra na sobra firme. */
        public long sobraCentavos() {
            return receitasCentavos - compromissosCentavos;
        }

        /** "Pode chegar a": a sobra se as entradas estimadas vierem no máximo. */
        public long sobraMaximaCentavos() {
            return sobraCentavos() + extraPossivelCentavos;
        }
    }

    /** O instante mais recente em que os dados da conta mudaram, para o "atualizado há N dias". */
    public static Optional<Instant> ultimaAtualizacao(Collection<Instant> instantes) {
        return instantes.stream().max(Instant::compareTo);
    }
}
