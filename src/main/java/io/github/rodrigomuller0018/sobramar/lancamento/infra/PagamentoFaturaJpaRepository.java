package io.github.rodrigomuller0018.sobramar.lancamento.infra;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PagamentoFaturaJpaRepository extends JpaRepository<PagamentoFaturaEntity, UUID> {

    List<PagamentoFaturaEntity> findByContaIdAndCartaoId(UUID contaId, UUID cartaoId);

    long deleteByContaIdAndCartaoIdAndCompetencia(UUID contaId, UUID cartaoId, String competencia);
}
