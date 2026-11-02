package io.github.rodrigomuller0018.sobramar.autenticacao.dominio;

import java.util.Optional;
import java.util.UUID;

/** Porta de persistência de usuários. */
public interface UsuarioRepository {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    Optional<Usuario> buscarPorEmail(String email);
}
