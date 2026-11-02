package io.github.rodrigomuller0018.sobramar.categoria.dominio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Porta de persistência. Toda consulta recebe a conta: não existe busca só por id. */
public interface CategoriaRepository {

    /** Lança ConflitoException se o nome já existir na conta. */
    Categoria salvar(Categoria categoria);

    Optional<Categoria> buscar(UUID contaId, UUID id);

    Page<Categoria> listar(UUID contaId, boolean incluirArquivadas, Pageable paginacao);
}
