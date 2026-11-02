package io.github.rodrigomuller0018.sobramar.compromisso.dominio;

import java.time.YearMonth;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** O que já foi pago. Implementada pelo módulo de lançamentos, que não é lido por dentro daqui. */
public interface PagamentosRealizados {

    /** Número da ocorrência para o valor real pago. */
    Map<Integer, Long> ocorrenciasPagas(UUID contaId, UUID compromissoId);

    Set<YearMonth> faturasPagas(UUID contaId, UUID cartaoId);
}
