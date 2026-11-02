package io.github.rodrigomuller0018.sobramar.pessoa.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pessoa")
class PessoaEntity {

    @Id
    private UUID id;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(nullable = false)
    private String apelido;

    @Column(nullable = false)
    private boolean arquivada;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected PessoaEntity() {
        // exigido pelo JPA
    }

    PessoaEntity(UUID id, UUID contaId, String apelido, boolean arquivada, Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.apelido = apelido;
        this.arquivada = arquivada;
        this.criadoEm = criadoEm;
    }

    UUID getId() {
        return id;
    }

    UUID getContaId() {
        return contaId;
    }

    String getApelido() {
        return apelido;
    }

    boolean isArquivada() {
        return arquivada;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
