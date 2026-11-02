package io.github.rodrigomuller0018.sobramar.pessoa.dominio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Porta de persistência. Toda consulta recebe a conta: não existe busca só por id. */
public interface PessoaRepository {

    /** Lança ConflitoException se o apelido já existir na conta. */
    Pessoa salvar(Pessoa pessoa);

    Optional<Pessoa> buscar(UUID contaId, UUID id);

    Page<Pessoa> listar(UUID contaId, boolean incluirArquivadas, Pageable paginacao);
}
