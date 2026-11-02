package io.github.rodrigomuller0018.sobramar.compromisso.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface CompromissoJpaRepository
        extends JpaRepository<CompromissoEntity, UUID>, JpaSpecificationExecutor<CompromissoEntity> {

    Optional<CompromissoEntity> findByContaIdAndId(UUID contaId, UUID id);

    List<CompromissoEntity> findByContaIdAndCartaoId(UUID contaId, UUID cartaoId);
}
