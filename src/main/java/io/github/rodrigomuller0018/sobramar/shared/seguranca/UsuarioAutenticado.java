package io.github.rodrigomuller0018.sobramar.shared.seguranca;

import java.util.UUID;

/** Quem fez a requisição. A conta vem sempre daqui, nunca da URL ou do corpo. */
public record UsuarioAutenticado(UUID usuarioId, UUID contaId) {}
