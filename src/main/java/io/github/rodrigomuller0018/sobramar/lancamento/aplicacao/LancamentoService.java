package io.github.rodrigomuller0018.sobramar.lancamento.aplicacao;

import io.github.rodrigomuller0018.sobramar.carteira.aplicacao.CarteiraService;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.Carteira;
import io.github.rodrigomuller0018.sobramar.categoria.aplicacao.CategoriaService;
import io.github.rodrigomuller0018.sobramar.compromisso.aplicacao.CompromissoService;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.Lancamento;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.LancamentoRepository;
import io.github.rodrigomuller0018.sobramar.pessoa.aplicacao.PessoaService;
import io.github.rodrigomuller0018.sobramar.shared.erro.RecursoNaoEncontradoException;
import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LancamentoService {

    public record Dados(
            Lancamento.Tipo tipo,
            String descricao,
            long valorCentavos,
            LocalDate data,
            UUID carteiraId,
            UUID carteiraDestinoId,
            UUID pessoaId,
            UUID categoriaId,
            boolean ignorado) {}

    public record Pagamento(LocalDate data, long valorCentavos, UUID carteiraId) {}

    private final LancamentoRepository repositorio;
    private final CompromissoService compromissos;
    private final CarteiraService carteiras;
    private final PessoaService pessoas;
    private final CategoriaService categorias;
    private final Clock clock;

    public LancamentoService(
            LancamentoRepository repositorio,
            CompromissoService compromissos,
            CarteiraService carteiras,
            PessoaService pessoas,
            CategoriaService categorias,
            Clock clock) {
        this.repositorio = repositorio;
        this.compromissos = compromissos;
        this.carteiras = carteiras;
        this.pessoas = pessoas;
        this.categorias = categorias;
        this.clock = clock;
    }

    @Transactional
    public Lancamento criar(UUID contaId, Dados d) {
        validarReferencias(contaId, d);
        return repositorio.salvar(new Lancamento(
                UUID.randomUUID(),
                contaId,
                d.tipo(),
                d.descricao().strip(),
                d.valorCentavos(),
                d.data(),
                d.carteiraId(),
                d.carteiraDestinoId(),
                null,
                null,
                d.pessoaId(),
                d.categoriaId(),
                d.ignorado(),
                clock.instant()));
    }

    /** De outra conta responde igual a inexistente, para não revelar que existe. */
    @Transactional(readOnly = true)
    public Lancamento buscar(UUID contaId, UUID id) {
        return repositorio.buscar(contaId, id).orElseThrow(LancamentoService::naoEncontrado);
    }

    @Transactional(readOnly = true)
    public Page<Lancamento> listar(UUID contaId, LancamentoRepository.Filtro filtro, Pageable paginacao) {
        return repositorio.listar(contaId, filtro, paginacao);
    }

    @Transactional
    public Lancamento atualizar(UUID contaId, UUID id, Dados d) {
        Lancamento atual = buscar(contaId, id);
        validarReferencias(contaId, d);
        return repositorio.salvar(new Lancamento(
                id,
                contaId,
                d.tipo(),
                d.descricao().strip(),
                d.valorCentavos(),
                d.data(),
                d.carteiraId(),
                d.carteiraDestinoId(),
                atual.compromissoId(),
                atual.numeroOcorrencia(),
                d.pessoaId(),
                d.categoriaId(),
                d.ignorado(),
                atual.criadoEm()));
    }

    @Transactional
    public void excluir(UUID contaId, UUID id) {
        repositorio.excluir(buscar(contaId, id));
    }

    /** O realizado substitui o previsto daquela ocorrência. No cartão, o que se paga é a fatura. */
    @Transactional
    public Lancamento pagarOcorrencia(UUID contaId, UUID compromissoId, int numero, Pagamento p) {
        Compromisso compromisso = compromissos.buscar(contaId, compromissoId);
        compromissos.ocorrencia(contaId, compromisso, numero);
        if (compromisso.noCartao()) {
            throw new RegraDeNegocioException("OCORRENCIA_EM_CARTAO", "Parcela no cartão é paga junto com a fatura.");
        }
        Carteira carteira = carteiras.buscarCarteira(contaId, p.carteiraId());
        CompromissoService.garantirCarteiraCompativel(
                carteiras.buscarCarteira(contaId, compromisso.carteiraId()), carteira);
        Lancamento.Tipo tipo = compromisso.natureza() == Compromisso.Natureza.ENTRADA
                ? Lancamento.Tipo.RECEITA
                : Lancamento.Tipo.GASTO;
        return repositorio.salvar(new Lancamento(
                UUID.randomUUID(),
                contaId,
                tipo,
                compromisso.descricao(),
                p.valorCentavos(),
                p.data(),
                carteira.getId(),
                null,
                compromissoId,
                numero,
                compromisso.pessoaId(),
                compromisso.categoriaId(),
                false,
                clock.instant()));
    }

    @Transactional
    public void desfazerPagamentoDaOcorrencia(UUID contaId, UUID compromissoId, int numero) {
        compromissos.buscar(contaId, compromissoId);
        repositorio.excluir(repositorio
                .buscarDaOcorrencia(contaId, compromissoId, numero)
                .orElseThrow(LancamentoService::naoEncontrado));
    }

    /** Fica fora da sobra: as compras já foram contadas nas ocorrências. */
    @Transactional
    public void pagarFatura(UUID contaId, UUID cartaoId, YearMonth competencia, Pagamento p) {
        carteiras.buscarCartao(contaId, cartaoId);
        Carteira carteira = carteiras.buscarCarteira(contaId, p.carteiraId());
        if (carteira.getTipo() == Carteira.Tipo.VALE) {
            throw new RegraDeNegocioException("CARTEIRA_INCOMPATIVEL", "Fatura não se paga com vale.");
        }
        if (p.valorCentavos() <= 0) {
            throw new RegraDeNegocioException("VALOR_INVALIDO", "O valor precisa ser maior que zero.");
        }
        repositorio.registrarPagamentoDeFatura(
                contaId, cartaoId, competencia, p.valorCentavos(), p.data(), carteira.getId());
    }

    /** "Bate com minha fatura?": o total que o banco cobrou, para conciliar antes de pagar. Não é pagamento. */
    @Transactional
    public void informarTotalDaFatura(UUID contaId, UUID cartaoId, YearMonth competencia, long totalCentavos) {
        carteiras.buscarCartao(contaId, cartaoId);
        if (totalCentavos <= 0) {
            throw new RegraDeNegocioException("VALOR_INVALIDO", "O valor precisa ser maior que zero.");
        }
        repositorio.informarTotalDaFatura(contaId, cartaoId, competencia, totalCentavos);
    }

    @Transactional
    public void esquecerTotalDaFatura(UUID contaId, UUID cartaoId, YearMonth competencia) {
        carteiras.buscarCartao(contaId, cartaoId);
        if (!repositorio.esquecerTotalDaFatura(contaId, cartaoId, competencia)) {
            throw naoEncontrado();
        }
    }

    @Transactional
    public void desfazerPagamentoDeFatura(UUID contaId, UUID cartaoId, YearMonth competencia) {
        carteiras.buscarCartao(contaId, cartaoId);
        if (!repositorio.excluirPagamentoDeFatura(contaId, cartaoId, competencia)) {
            throw naoEncontrado();
        }
    }

    private void validarReferencias(UUID contaId, Dados d) {
        Carteira origem = carteiras.buscarCarteira(contaId, d.carteiraId());
        if (d.carteiraDestinoId() != null) {
            CompromissoService.garantirCarteiraCompativel(
                    origem, carteiras.buscarCarteira(contaId, d.carteiraDestinoId()));
        }
        if (d.pessoaId() != null) {
            pessoas.buscar(contaId, d.pessoaId());
        }
        if (d.categoriaId() != null) {
            categorias.buscar(contaId, d.categoriaId());
        }
    }

    private static RecursoNaoEncontradoException naoEncontrado() {
        return new RecursoNaoEncontradoException("RECURSO_NAO_ENCONTRADO", "Recurso não encontrado.");
    }
}
