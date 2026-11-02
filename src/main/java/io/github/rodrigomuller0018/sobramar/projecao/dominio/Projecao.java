package io.github.rodrigomuller0018.sobramar.projecao.dominio;

import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** O resultado da conta: a linha do tempo, mês a mês. */
public final class Projecao {

    private Projecao() {}

    public enum Situacao {
        PREVISTO,
        REALIZADO
    }

    public record Item(
            UUID compromissoId,
            Integer numeroOcorrencia,
            Integer totalOcorrencias,
            String descricao,
            Compromisso.Natureza natureza,
            Compromisso.Certeza certeza,
            long valorCentavos,
            LocalDate dataCompra,
            LocalDate vencimento,
            boolean ignorada,
            UUID cartaoId,
            UUID carteiraId,
            UUID pessoaId,
            UUID categoriaId,
            Situacao situacao) {}

    public record Fatura(UUID cartaoId, LocalDate vencimento, long totalCentavos, boolean paga) {}

    public record Vale(UUID carteiraId, long creditosCentavos, long gastosCentavos) {}

    public record Mes(
            YearMonth competencia,
            long receitasCentavos,
            long compromissosCentavos,
            boolean contemEstimativa,
            List<Fatura> faturas,
            List<Item> itens,
            List<Vale> vale) {

        /** Pode ser negativa. */
        public long sobraCentavos() {
            return receitasCentavos - compromissosCentavos;
        }
    }
}
