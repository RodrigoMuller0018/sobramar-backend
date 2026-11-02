package io.github.rodrigomuller0018.sobramar.autenticacao.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
class UsuarioEntity {

    @Id
    private UUID id;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "totp_segredo_cifrado")
    private String totpSegredoCifrado;

    @Column(name = "totp_ativo", nullable = false)
    private boolean totpAtivo;

    @Column(name = "totp_ultimo_passo")
    private Long totpUltimoPasso;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected UsuarioEntity() {
        // exigido pelo JPA
    }

    UsuarioEntity(
            UUID id,
            UUID contaId,
            String email,
            String senhaHash,
            String totpSegredoCifrado,
            boolean totpAtivo,
            Long totpUltimoPasso,
            Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.email = email;
        this.senhaHash = senhaHash;
        this.totpSegredoCifrado = totpSegredoCifrado;
        this.totpAtivo = totpAtivo;
        this.totpUltimoPasso = totpUltimoPasso;
        this.criadoEm = criadoEm;
    }

    UUID getId() {
        return id;
    }

    UUID getContaId() {
        return contaId;
    }

    String getEmail() {
        return email;
    }

    String getSenhaHash() {
        return senhaHash;
    }

    String getTotpSegredoCifrado() {
        return totpSegredoCifrado;
    }

    boolean isTotpAtivo() {
        return totpAtivo;
    }

    Long getTotpUltimoPasso() {
        return totpUltimoPasso;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
