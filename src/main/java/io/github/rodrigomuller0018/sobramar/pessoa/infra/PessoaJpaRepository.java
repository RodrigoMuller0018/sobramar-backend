package io.github.rodrigomuller0018.sobramar.pessoa.infra;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PessoaJpaRepository extends JpaRepository<PessoaEntity, UUID> {

    Optional<PessoaEntity> findByContaIdAndId(UUID contaId, UUID id);

    Page<PessoaEntity> findByContaId(UUID contaId, Pageable paginacao);

    Page<PessoaEntity> findByContaIdAndArquivadaFalse(UUID contaId, Pageable paginacao);
}
