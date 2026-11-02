package io.github.rodrigomuller0018.sobramar.carteira.dominio;

import java.util.UUID;

/** Quanto do limite do cartão está comprometido. Implementada pelo módulo de compromissos. */
@FunctionalInterface
public interface UsoDoLimite {

    long limiteUsadoCentavos(UUID contaId, UUID cartaoId);
}
