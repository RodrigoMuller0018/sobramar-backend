package io.github.rodrigomuller0018.sobramar.autenticacao.infra;

import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.Usuario;
import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador: traduz entre o domínio e o modelo JPA. */
@Repository
class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;

    UsuarioRepositoryAdapter(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Usuario salvar(Usuario u) {
        return paraDominio(jpa.save(new UsuarioEntity(
                u.getId(),
                u.getContaId(),
                u.getEmail(),
                u.getSenhaHash(),
                u.getTotpSegredoCifrado(),
                u.isTotpAtivo(),
                u.getTotpUltimoPasso(),
                u.getCriadoEm())));
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id).map(UsuarioRepositoryAdapter::paraDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpa.findByEmail(email).map(UsuarioRepositoryAdapter::paraDominio);
    }

    private static Usuario paraDominio(UsuarioEntity e) {
        return Usuario.restaurar(
                e.getId(),
                e.getContaId(),
                e.getEmail(),
                e.getSenhaHash(),
                e.getCriadoEm(),
                e.getTotpSegredoCifrado(),
                e.isTotpAtivo(),
                e.getTotpUltimoPasso());
    }
}
