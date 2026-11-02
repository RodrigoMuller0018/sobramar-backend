package io.github.rodrigomuller0018.sobramar.carteira.infra;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartaoJpaRepository extends JpaRepository<CartaoEntity, UUID> {

    Optional<CartaoEntity> findByContaIdAndId(UUID contaId, UUID id);

    Page<CartaoEntity> findByContaId(UUID contaId, Pageable paginacao);

    Page<CartaoEntity> findByContaIdAndArquivadoFalse(UUID contaId, Pageable paginacao);
}
