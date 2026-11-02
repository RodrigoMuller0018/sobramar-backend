package io.github.rodrigomuller0018.template.tarefa.aplicacao;

import io.github.rodrigomuller0018.template.shared.erro.RecursoNaoEncontradoException;
import io.github.rodrigomuller0018.template.tarefa.dominio.Tarefa;
import io.github.rodrigomuller0018.template.tarefa.dominio.TarefaRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestra os casos de uso; a regra de negócio mora em {@link Tarefa}. */
@Service
public class TarefaService {

    private final TarefaRepository repositorio;
    private final Clock clock;

    public TarefaService(TarefaRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Transactional
    public Tarefa criar(String titulo) {
        return repositorio.salvar(Tarefa.nova(titulo, clock));
    }

    @Transactional(readOnly = true)
    public Tarefa buscar(UUID id) {
        return repositorio
                .buscarPorId(id)
                .orElseThrow(
                        () -> new RecursoNaoEncontradoException("TAREFA_NAO_ENCONTRADA", "Tarefa não encontrada."));
    }

    @Transactional(readOnly = true)
    public Page<Tarefa> listar(Pageable paginacao) {
        return repositorio.listar(paginacao);
    }

    @Transactional
    public Tarefa concluir(UUID id) {
        Tarefa tarefa = buscar(id);
        tarefa.concluir(clock);
        return repositorio.salvar(tarefa);
    }
}
