package io.github.rodrigomuller0018.sobramar.categoria.infra;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, UUID> {

    Optional<CategoriaEntity> findByContaIdAndId(UUID contaId, UUID id);

    Page<CategoriaEntity> findByContaId(UUID contaId, Pageable paginacao);

    Page<CategoriaEntity> findByContaIdAndArquivadaFalse(UUID contaId, Pageable paginacao);
}
