package io.github.rodrigomuller0018.template.tarefa.infra;

import io.github.rodrigomuller0018.template.tarefa.dominio.Tarefa;
import io.github.rodrigomuller0018.template.tarefa.dominio.TarefaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** Adaptador: traduz entre o domínio e o modelo JPA. */
@Repository
class TarefaRepositoryAdapter implements TarefaRepository {

    private final TarefaJpaRepository jpa;

    TarefaRepositoryAdapter(TarefaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Tarefa salvar(Tarefa tarefa) {
        return paraDominio(jpa.save(paraEntidade(tarefa)));
    }

    @Override
    public Optional<Tarefa> buscarPorId(UUID id) {
        return jpa.findById(id).map(TarefaRepositoryAdapter::paraDominio);
    }

    @Override
    public Page<Tarefa> listar(Pageable paginacao) {
        return jpa.findAll(paginacao).map(TarefaRepositoryAdapter::paraDominio);
    }

    private static TarefaEntity paraEntidade(Tarefa t) {
        return new TarefaEntity(t.getId(), t.getTitulo(), t.getCriadaEm(), t.getConcluidaEm());
    }

    private static Tarefa paraDominio(TarefaEntity e) {
        return Tarefa.restaurar(e.getId(), e.getTitulo(), e.getCriadaEm(), e.getConcluidaEm());
    }
}
