package io.github.rodrigomuller0018.sobramar.lancamento.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FaturaInformadaJpaRepository extends JpaRepository<FaturaInformadaEntity, UUID> {

    List<FaturaInformadaEntity> findByContaIdAndCartaoId(UUID contaId, UUID cartaoId);

    Optional<FaturaInformadaEntity> findByContaIdAndCartaoIdAndCompetencia(
            UUID contaId, UUID cartaoId, String competencia);

    long deleteByContaIdAndCartaoIdAndCompetencia(UUID contaId, UUID cartaoId, String competencia);
}
