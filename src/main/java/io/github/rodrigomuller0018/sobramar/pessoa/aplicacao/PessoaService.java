package io.github.rodrigomuller0018.sobramar.pessoa.aplicacao;

import io.github.rodrigomuller0018.sobramar.pessoa.dominio.Pessoa;
import io.github.rodrigomuller0018.sobramar.pessoa.dominio.PessoaRepository;
import io.github.rodrigomuller0018.sobramar.shared.erro.RecursoNaoEncontradoException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PessoaService {

    private final PessoaRepository repositorio;
    private final Clock clock;

    public PessoaService(PessoaRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Transactional
    public Pessoa criar(UUID contaId, String apelido) {
        return repositorio.salvar(Pessoa.nova(contaId, apelido, clock.instant()));
    }

    /** Pessoa de outra conta responde igual a uma inexistente, para não revelar que ela existe. */
    @Transactional(readOnly = true)
    public Pessoa buscar(UUID contaId, UUID id) {
        return repositorio
                .buscar(contaId, id)
                .orElseThrow(
                        () -> new RecursoNaoEncontradoException("RECURSO_NAO_ENCONTRADO", "Recurso não encontrado."));
    }

    @Transactional(readOnly = true)
    public Page<Pessoa> listar(UUID contaId, boolean incluirArquivadas, Pageable paginacao) {
        return repositorio.listar(contaId, incluirArquivadas, paginacao);
    }

    @Transactional
    public Pessoa renomear(UUID contaId, UUID id, String apelido) {
        Pessoa pessoa = buscar(contaId, id);
        pessoa.renomear(apelido);
        return repositorio.salvar(pessoa);
    }

    /** Arquiva em vez de apagar, para preservar o histórico. */
    @Transactional
    public void arquivar(UUID contaId, UUID id) {
        Pessoa pessoa = buscar(contaId, id);
        pessoa.arquivar();
        repositorio.salvar(pessoa);
    }
}
