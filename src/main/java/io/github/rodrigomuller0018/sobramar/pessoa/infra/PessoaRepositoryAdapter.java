package io.github.rodrigomuller0018.sobramar.pessoa.infra;

import io.github.rodrigomuller0018.sobramar.pessoa.dominio.Pessoa;
import io.github.rodrigomuller0018.sobramar.pessoa.dominio.PessoaRepository;
import io.github.rodrigomuller0018.sobramar.shared.erro.ConflitoException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** Adaptador: traduz entre o domínio e o modelo JPA. */
@Repository
class PessoaRepositoryAdapter implements PessoaRepository {

    private final PessoaJpaRepository jpa;

    PessoaRepositoryAdapter(PessoaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Pessoa salvar(Pessoa p) {
        try {
            return paraDominio(jpa.saveAndFlush(
                    new PessoaEntity(p.getId(), p.getContaId(), p.getApelido(), p.isArquivada(), p.getCriadoEm())));
        } catch (DataIntegrityViolationException e) {
            // O índice único é a garantia real; checar antes não evitaria a corrida entre duas requisições.
            throw new ConflitoException("APELIDO_DUPLICADO", "Já existe uma pessoa com esse apelido.");
        }
    }

    @Override
    public Optional<Pessoa> buscar(UUID contaId, UUID id) {
        return jpa.findByContaIdAndId(contaId, id).map(PessoaRepositoryAdapter::paraDominio);
    }

    @Override
    public Page<Pessoa> listar(UUID contaId, boolean incluirArquivadas, Pageable paginacao) {
        Page<PessoaEntity> pagina = incluirArquivadas
                ? jpa.findByContaId(contaId, paginacao)
                : jpa.findByContaIdAndArquivadaFalse(contaId, paginacao);
        return pagina.map(PessoaRepositoryAdapter::paraDominio);
    }

    private static Pessoa paraDominio(PessoaEntity e) {
        return Pessoa.restaurar(e.getId(), e.getContaId(), e.getApelido(), e.isArquivada(), e.getCriadoEm());
    }
}
