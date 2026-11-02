package io.github.rodrigomuller0018.sobramar.carteira.aplicacao;

import io.github.rodrigomuller0018.sobramar.carteira.dominio.Cartao;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.Carteira;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.CarteiraRepository;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.shared.erro.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** De outra conta responde igual a inexistente, para não revelar que existe. */
@Service
public class CarteiraService {

    private final CarteiraRepository repositorio;
    private final Clock clock;

    public CarteiraService(CarteiraRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Transactional
    public Carteira criarCarteira(UUID contaId, Carteira.Tipo tipo, String nome, long saldoInicialCentavos) {
        return repositorio.salvarCarteira(Carteira.nova(contaId, tipo, nome, saldoInicialCentavos, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Carteira buscarCarteira(UUID contaId, UUID id) {
        return repositorio.buscarCarteira(contaId, id).orElseThrow(CarteiraService::naoEncontrado);
    }

    @Transactional(readOnly = true)
    public Page<Carteira> listarCarteiras(UUID contaId, boolean incluirArquivadas, Pageable paginacao) {
        return repositorio.listarCarteiras(contaId, incluirArquivadas, paginacao);
    }

    @Transactional
    public Carteira atualizarCarteira(
            UUID contaId, UUID id, Carteira.Tipo tipo, String nome, long saldoInicialCentavos) {
        Carteira carteira = buscarCarteira(contaId, id);
        carteira.atualizar(tipo, nome, saldoInicialCentavos);
        return repositorio.salvarCarteira(carteira);
    }

    /** Arquiva em vez de apagar, para preservar o histórico. */
    @Transactional
    public void arquivarCarteira(UUID contaId, UUID id) {
        Carteira carteira = buscarCarteira(contaId, id);
        carteira.arquivar();
        repositorio.salvarCarteira(carteira);
    }

    @Transactional
    public Cartao criarCartao(UUID contaId, String nome, RegraDeFatura regra, Long limiteCentavos) {
        return repositorio.salvarCartao(Cartao.novo(contaId, nome, regra, limiteCentavos, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Cartao buscarCartao(UUID contaId, UUID id) {
        return repositorio.buscarCartao(contaId, id).orElseThrow(CarteiraService::naoEncontrado);
    }

    @Transactional(readOnly = true)
    public Page<Cartao> listarCartoes(UUID contaId, boolean incluirArquivados, Pageable paginacao) {
        return repositorio.listarCartoes(contaId, incluirArquivados, paginacao);
    }

    @Transactional
    public Cartao atualizarCartao(UUID contaId, UUID id, String nome, RegraDeFatura regra, Long limiteCentavos) {
        Cartao cartao = buscarCartao(contaId, id);
        cartao.atualizar(nome, regra, limiteCentavos);
        return repositorio.salvarCartao(cartao);
    }

    @Transactional
    public void arquivarCartao(UUID contaId, UUID id) {
        Cartao cartao = buscarCartao(contaId, id);
        cartao.arquivar();
        repositorio.salvarCartao(cartao);
    }

    @Transactional(readOnly = true)
    public RegraDeFatura.Fatura faturaSugerida(UUID contaId, UUID cartaoId, LocalDate dataCompra) {
        return buscarCartao(contaId, cartaoId).getRegra().faturaDaCompra(dataCompra);
    }

    private static RecursoNaoEncontradoException naoEncontrado() {
        return new RecursoNaoEncontradoException("RECURSO_NAO_ENCONTRADO", "Recurso não encontrado.");
    }
}
