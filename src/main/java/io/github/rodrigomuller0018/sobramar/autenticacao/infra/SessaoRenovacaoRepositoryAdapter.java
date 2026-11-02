package io.github.rodrigomuller0018.sobramar.autenticacao.infra;

import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.SessaoRenovacao;
import io.github.rodrigomuller0018.sobramar.autenticacao.dominio.SessaoRenovacaoRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador: traduz entre o domínio e o modelo JPA. */
@Repository
class SessaoRenovacaoRepositoryAdapter implements SessaoRenovacaoRepository {

    private final SessaoRenovacaoJpaRepository jpa;

    SessaoRenovacaoRepositoryAdapter(SessaoRenovacaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void criar(SessaoRenovacao s) {
        jpa.save(new SessaoRenovacaoEntity(
                s.id(), s.usuarioId(), s.tokenHash(), s.expiraEm(), s.revogadaEm(), s.criadoEm()));
    }

    @Override
    public Optional<SessaoRenovacao> buscarPorHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash)
                .map(e -> new SessaoRenovacao(
                        e.getId(),
                        e.getUsuarioId(),
                        e.getTokenHash(),
                        e.getExpiraEm(),
                        e.getRevogadaEm(),
                        e.getCriadoEm()));
    }

    @Override
    public void revogar(UUID sessaoId, Instant agora) {
        jpa.revogar(sessaoId, agora);
    }

    @Override
    public void revogarTodasDoUsuario(UUID usuarioId, Instant agora) {
        jpa.revogarTodas(usuarioId, agora);
    }
}
