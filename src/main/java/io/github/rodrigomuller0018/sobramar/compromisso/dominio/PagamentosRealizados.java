package io.github.rodrigomuller0018.sobramar.compromisso.dominio;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** O que já foi pago. Implementada pelo módulo de lançamentos, que não é lido por dentro daqui. */
public interface PagamentosRealizados {

    /** Uma fatura paga: o valor é o total que o banco cobrou, e serve para conciliar a fatura calculada. */
    record FaturaPaga(YearMonth competencia, long valorCentavos, Instant pagaEm) {}

    /** Número da ocorrência para o valor real pago. */
    Map<Integer, Long> ocorrenciasPagas(UUID contaId, UUID compromissoId);

    List<FaturaPaga> faturasPagasDetalhadas(UUID contaId, UUID cartaoId);

    default Set<YearMonth> faturasPagas(UUID contaId, UUID cartaoId) {
        return faturasPagasDetalhadas(contaId, cartaoId).stream()
                .map(FaturaPaga::competencia)
                .collect(Collectors.toSet());
    }
}
