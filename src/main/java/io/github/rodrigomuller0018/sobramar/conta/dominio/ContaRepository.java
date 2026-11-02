package io.github.rodrigomuller0018.sobramar.conta.dominio;

import java.util.Optional;
import java.util.UUID;

/** Porta de persistência de contas. */
public interface ContaRepository {

    void criar(Conta conta);

    Optional<Conta> buscarPorId(UUID id);
}
