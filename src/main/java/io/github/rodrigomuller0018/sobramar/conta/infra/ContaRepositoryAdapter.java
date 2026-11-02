package io.github.rodrigomuller0018.sobramar.conta.infra;

import io.github.rodrigomuller0018.sobramar.conta.dominio.Conta;
import io.github.rodrigomuller0018.sobramar.conta.dominio.ContaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador: traduz entre o domínio e o modelo JPA. */
@Repository
class ContaRepositoryAdapter implements ContaRepository {

    private final ContaJpaRepository jpa;

    ContaRepositoryAdapter(ContaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void criar(Conta conta) {
        jpa.save(new ContaEntity(conta.id(), conta.nome(), conta.criadoEm()));
    }

    @Override
    public Optional<Conta> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> new Conta(e.getId(), e.getNome(), e.getCriadoEm()));
    }
}
