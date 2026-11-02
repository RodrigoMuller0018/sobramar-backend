package io.github.rodrigomuller0018.sobramar.autenticacao.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessao_renovacao")
class SessaoRenovacaoEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "revogada_em")
    private Instant revogadaEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected SessaoRenovacaoEntity() {
        // exigido pelo JPA
    }

    SessaoRenovacaoEntity(
            UUID id, UUID usuarioId, String tokenHash, Instant expiraEm, Instant revogadaEm, Instant criadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
        this.revogadaEm = revogadaEm;
        this.criadoEm = criadoEm;
    }

    UUID getId() {
        return id;
    }

    UUID getUsuarioId() {
        return usuarioId;
    }

    String getTokenHash() {
        return tokenHash;
    }

    Instant getExpiraEm() {
        return expiraEm;
    }

    Instant getRevogadaEm() {
        return revogadaEm;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
