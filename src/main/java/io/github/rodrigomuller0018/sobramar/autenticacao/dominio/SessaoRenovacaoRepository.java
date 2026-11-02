package io.github.rodrigomuller0018.sobramar.autenticacao.dominio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das sessões de renovação. */
public interface SessaoRenovacaoRepository {

    void criar(SessaoRenovacao sessao);

    Optional<SessaoRenovacao> buscarPorHash(String tokenHash);

    void revogar(UUID sessaoId, Instant agora);

    void revogarTodasDoUsuario(UUID usuarioId, Instant agora);
}
