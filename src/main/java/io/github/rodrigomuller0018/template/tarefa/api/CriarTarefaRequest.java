package io.github.rodrigomuller0018.template.tarefa.api;

import io.github.rodrigomuller0018.template.tarefa.dominio.Tarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada: validação de formato na borda; a regra de negócio fica no domínio. */
public record CriarTarefaRequest(
        @NotBlank @Size(max = Tarefa.TAMANHO_MAXIMO_TITULO) String titulo) {}
