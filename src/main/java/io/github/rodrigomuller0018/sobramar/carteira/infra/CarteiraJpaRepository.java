package io.github.rodrigomuller0018.sobramar.carteira.infra;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CarteiraJpaRepository extends JpaRepository<CarteiraEntity, UUID> {

    Optional<CarteiraEntity> findByContaIdAndId(UUID contaId, UUID id);

    Page<CarteiraEntity> findByContaId(UUID contaId, Pageable paginacao);

    Page<CarteiraEntity> findByContaIdAndArquivadaFalse(UUID contaId, Pageable paginacao);
}
