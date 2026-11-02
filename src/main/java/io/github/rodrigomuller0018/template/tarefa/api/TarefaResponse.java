package io.github.rodrigomuller0018.template.tarefa.api;

import io.github.rodrigomuller0018.template.tarefa.dominio.Tarefa;
import java.time.Instant;
import java.util.UUID;

/** DTO de saída: a entidade nunca vaza pela API. */
public record TarefaResponse(UUID id, String titulo, boolean concluida, Instant criadaEm, Instant concluidaEm) {

    static TarefaResponse de(Tarefa tarefa) {
        return new TarefaResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.isConcluida(),
                tarefa.getCriadaEm(),
                tarefa.getConcluidaEm());
    }
}
