package io.github.rodrigomuller0018.sobramar.categoria.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categoria")
class CategoriaEntity {

    @Id
    private UUID id;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean arquivada;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected CategoriaEntity() {
        // exigido pelo JPA
    }

    CategoriaEntity(UUID id, UUID contaId, String nome, boolean arquivada, Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.nome = nome;
        this.arquivada = arquivada;
        this.criadoEm = criadoEm;
    }

    UUID getId() {
        return id;
    }

    UUID getContaId() {
        return contaId;
    }

    String getNome() {
        return nome;
    }

    boolean isArquivada() {
        return arquivada;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
