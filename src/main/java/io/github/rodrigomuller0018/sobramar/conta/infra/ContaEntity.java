package io.github.rodrigomuller0018.sobramar.conta.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conta")
class ContaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected ContaEntity() {
        // exigido pelo JPA
    }

    ContaEntity(UUID id, String nome, Instant criadoEm) {
        this.id = id;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    UUID getId() {
        return id;
    }

    String getNome() {
        return nome;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
