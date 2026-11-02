package io.github.rodrigomuller0018.sobramar.compromisso.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OcorrenciaAjusteJpaRepository extends JpaRepository<OcorrenciaAjusteEntity, UUID> {

    List<OcorrenciaAjusteEntity> findByCompromissoId(UUID compromissoId);

    Optional<OcorrenciaAjusteEntity> findByCompromissoIdAndNumero(UUID compromissoId, int numero);
}
