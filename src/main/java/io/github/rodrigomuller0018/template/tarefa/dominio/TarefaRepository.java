package io.github.rodrigomuller0018.template.tarefa.dominio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Porta de saída para persistência. O banco é uma integração real, por isso tem porta e adaptador
 * (ver tarefa.infra). Page/Pageable vêm do spring-data-commons: um acoplamento pragmático e aceito.
 */
public interface TarefaRepository {

    Tarefa salvar(Tarefa tarefa);

    Optional<Tarefa> buscarPorId(UUID id);

    Page<Tarefa> listar(Pageable paginacao);
}
