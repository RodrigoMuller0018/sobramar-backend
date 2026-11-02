package io.github.rodrigomuller0018.sobramar.autenticacao.dominio;

import java.time.Instant;
import java.util.UUID;

/** Um refresh token emitido. Só o hash é guardado. */
public record SessaoRenovacao(
        UUID id, UUID usuarioId, String tokenHash, Instant expiraEm, Instant revogadaEm, Instant criadoEm) {

    public boolean revogada() {
        return revogadaEm != null;
    }

    public boolean expirada(Instant agora) {
        return !agora.isBefore(expiraEm);
    }
}
