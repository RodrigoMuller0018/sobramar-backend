package io.github.rodrigomuller0018.sobramar.categoria.aplicacao;

import io.github.rodrigomuller0018.sobramar.categoria.dominio.Categoria;
import io.github.rodrigomuller0018.sobramar.categoria.dominio.CategoriaRepository;
import io.github.rodrigomuller0018.sobramar.shared.erro.RecursoNaoEncontradoException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private final CategoriaRepository repositorio;
    private final Clock clock;

    public CategoriaService(CategoriaRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Transactional
    public void criarPadrao(UUID contaId) {
        Categoria.PADRAO.forEach(nome -> repositorio.salvar(Categoria.nova(contaId, nome, clock.instant())));
    }

    @Transactional
    public Categoria criar(UUID contaId, String nome) {
        return repositorio.salvar(Categoria.nova(contaId, nome, clock.instant()));
    }

    /** De outra conta responde igual a inexistente, para não revelar que existe. */
    @Transactional(readOnly = true)
    public Categoria buscar(UUID contaId, UUID id) {
        return repositorio
                .buscar(contaId, id)
                .orElseThrow(
                        () -> new RecursoNaoEncontradoException("RECURSO_NAO_ENCONTRADO", "Recurso não encontrado."));
    }

    @Transactional(readOnly = true)
    public Page<Categoria> listar(UUID contaId, boolean incluirArquivadas, Pageable paginacao) {
        return repositorio.listar(contaId, incluirArquivadas, paginacao);
    }

    @Transactional
    public Categoria renomear(UUID contaId, UUID id, String nome) {
        Categoria categoria = buscar(contaId, id);
        categoria.renomear(nome);
        return repositorio.salvar(categoria);
    }

    /** Arquiva em vez de apagar, para preservar o histórico. */
    @Transactional
    public void arquivar(UUID contaId, UUID id) {
        Categoria categoria = buscar(contaId, id);
        categoria.arquivar();
        repositorio.salvar(categoria);
    }
}
