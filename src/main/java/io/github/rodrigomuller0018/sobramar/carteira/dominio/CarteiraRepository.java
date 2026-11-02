package io.github.rodrigomuller0018.sobramar.carteira.dominio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Porta de persistência. Toda consulta recebe a conta: não existe busca só por id. */
public interface CarteiraRepository {

    Carteira salvarCarteira(Carteira carteira);

    Optional<Carteira> buscarCarteira(UUID contaId, UUID id);

    Page<Carteira> listarCarteiras(UUID contaId, boolean incluirArquivadas, Pageable paginacao);

    Cartao salvarCartao(Cartao cartao);

    Optional<Cartao> buscarCartao(UUID contaId, UUID id);

    Page<Cartao> listarCartoes(UUID contaId, boolean incluirArquivados, Pageable paginacao);
}
