package io.github.rodrigomuller0018.template.tarefa.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Modelo de persistência. Fica na infraestrutura para o domínio não depender de JPA. */
@Entity
@Table(name = "tarefa")
class TarefaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "concluida_em")
    private Instant concluidaEm;

    protected TarefaEntity() {
        // exigido pelo JPA
    }

    TarefaEntity(UUID id, String titulo, Instant criadaEm, Instant concluidaEm) {
        this.id = id;
        this.titulo = titulo;
        this.criadaEm = criadaEm;
        this.concluidaEm = concluidaEm;
    }

    UUID getId() {
        return id;
    }

    String getTitulo() {
        return titulo;
    }

    Instant getCriadaEm() {
        return criadaEm;
    }

    Instant getConcluidaEm() {
        return concluidaEm;
    }
}
