package io.github.rodrigomuller0018.sobramar.lancamento.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface LancamentoJpaRepository
        extends JpaRepository<LancamentoEntity, UUID>, JpaSpecificationExecutor<LancamentoEntity> {

    Optional<LancamentoEntity> findByContaIdAndId(UUID contaId, UUID id);

    Optional<LancamentoEntity> findByContaIdAndCompromissoIdAndNumeroOcorrencia(
            UUID contaId, UUID compromissoId, int numeroOcorrencia);

    List<LancamentoEntity> findByContaIdAndCompromissoId(UUID contaId, UUID compromissoId);
}
