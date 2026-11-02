package io.github.rodrigomuller0018.sobramar.carteira.infra;

import io.github.rodrigomuller0018.sobramar.carteira.dominio.Cartao;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.Carteira;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.CarteiraRepository;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.shared.dominio.Dinheiro;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** Adaptador: traduz entre o domínio e o modelo JPA. */
@Repository
class CarteiraRepositoryAdapter implements CarteiraRepository {

    private final CarteiraJpaRepository carteiras;
    private final CartaoJpaRepository cartoes;

    CarteiraRepositoryAdapter(CarteiraJpaRepository carteiras, CartaoJpaRepository cartoes) {
        this.carteiras = carteiras;
        this.cartoes = cartoes;
    }

    @Override
    public Carteira salvarCarteira(Carteira c) {
        return paraDominio(carteiras.save(new CarteiraEntity(
                c.getId(),
                c.getContaId(),
                c.getTipo().name(),
                c.getNome(),
                Dinheiro.paraBanco(c.getSaldoInicialCentavos()),
                c.isArquivada(),
                c.getCriadoEm())));
    }

    @Override
    public Optional<Carteira> buscarCarteira(UUID contaId, UUID id) {
        return carteiras.findByContaIdAndId(contaId, id).map(CarteiraRepositoryAdapter::paraDominio);
    }

    @Override
    public Page<Carteira> listarCarteiras(UUID contaId, boolean incluirArquivadas, Pageable paginacao) {
        Page<CarteiraEntity> pagina = incluirArquivadas
                ? carteiras.findByContaId(contaId, paginacao)
                : carteiras.findByContaIdAndArquivadaFalse(contaId, paginacao);
        return pagina.map(CarteiraRepositoryAdapter::paraDominio);
    }

    @Override
    public Cartao salvarCartao(Cartao c) {
        return paraDominio(cartoes.save(new CartaoEntity(
                c.getId(),
                c.getContaId(),
                c.getNome(),
                c.getRegra().diaFechamento(),
                c.getRegra().diaVencimento(),
                c.getLimiteCentavos() == null ? null : Dinheiro.paraBanco(c.getLimiteCentavos()),
                c.isArquivado(),
                c.getCriadoEm())));
    }

    @Override
    public Optional<Cartao> buscarCartao(UUID contaId, UUID id) {
        return cartoes.findByContaIdAndId(contaId, id).map(CarteiraRepositoryAdapter::paraDominio);
    }

    @Override
    public Page<Cartao> listarCartoes(UUID contaId, boolean incluirArquivados, Pageable paginacao) {
        Page<CartaoEntity> pagina = incluirArquivados
                ? cartoes.findByContaId(contaId, paginacao)
                : cartoes.findByContaIdAndArquivadoFalse(contaId, paginacao);
        return pagina.map(CarteiraRepositoryAdapter::paraDominio);
    }

    private static Carteira paraDominio(CarteiraEntity e) {
        return Carteira.restaurar(
                e.getId(),
                e.getContaId(),
                Carteira.Tipo.valueOf(e.getTipo()),
                e.getNome(),
                Dinheiro.deBanco(e.getSaldoInicial()),
                e.isArquivada(),
                e.getCriadoEm());
    }

    private static Cartao paraDominio(CartaoEntity e) {
        return Cartao.restaurar(
                e.getId(),
                e.getContaId(),
                e.getNome(),
                new RegraDeFatura(e.getDiaFechamento(), e.getDiaVencimento()),
                e.getLimite() == null ? null : Dinheiro.deBanco(e.getLimite()),
                e.isArquivado(),
                e.getCriadoEm());
    }
}
