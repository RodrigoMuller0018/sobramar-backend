package io.github.rodrigomuller0018.sobramar.autenticacao.infra;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SessaoRenovacaoJpaRepository extends JpaRepository<SessaoRenovacaoEntity, UUID> {

    Optional<SessaoRenovacaoEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SessaoRenovacaoEntity s set s.revogadaEm = :agora where s.id = :id and s.revogadaEm is null")
    void revogar(@Param("id") UUID id, @Param("agora") Instant agora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SessaoRenovacaoEntity s set s.revogadaEm = :agora "
            + "where s.usuarioId = :usuarioId and s.revogadaEm is null")
    void revogarTodas(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);
}
