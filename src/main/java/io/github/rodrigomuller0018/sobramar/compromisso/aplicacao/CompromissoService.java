package io.github.rodrigomuller0018.sobramar.compromisso.aplicacao;

import io.github.rodrigomuller0018.sobramar.carteira.aplicacao.CarteiraService;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.Carteira;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.UsoDoLimite;
import io.github.rodrigomuller0018.sobramar.categoria.aplicacao.CategoriaService;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.AjusteOcorrencia;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.CompromissoRepository;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.GeradorDeOcorrencias;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Ocorrencia;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.PagamentosRealizados;
import io.github.rodrigomuller0018.sobramar.pessoa.aplicacao.PessoaService;
import io.github.rodrigomuller0018.sobramar.shared.dominio.Calendario;
import io.github.rodrigomuller0018.sobramar.shared.erro.ConflitoException;
import io.github.rodrigomuller0018.sobramar.shared.erro.RecursoNaoEncontradoException;
import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompromissoService implements UsoDoLimite {

    private static final int MESES_A_FRENTE = 12;
    private static final String AVISO_FATURA = "FATURA_INCOMPATIVEL_COM_DATA";

    public enum Situacao {
        PREVISTO,
        REALIZADO
    }

    public enum Escopo {
        SOMENTE_ESTA,
        ESTA_E_FUTURAS,
        TODAS
    }

    public record Plano(
            Compromisso.Natureza natureza,
            Compromisso.Forma forma,
            Compromisso.Certeza certeza,
            String descricao,
            long valorCentavos,
            Integer parcelas,
            LocalDate dataInicio,
            LocalDate dataFim,
            boolean recorrenciaAnual,
            UUID carteiraId,
            UUID cartaoId,
            YearMonth faturaInicial,
            UUID pessoaId,
            UUID categoriaId) {}

    public record EdicaoDeOcorrencia(
            Escopo escopo, Long valorCentavos, LocalDate vencimento, YearMonth fatura, Boolean ignorada) {}

    public record OcorrenciaDetalhada(Ocorrencia ocorrencia, Situacao situacao, long valorCentavos) {}

    public record Resultado(Compromisso compromisso, List<String> avisos) {}

    public record Detalhe(Compromisso compromisso, List<OcorrenciaDetalhada> ocorrencias) {}

    private final CompromissoRepository repositorio;
    private final PagamentosRealizados pagamentos;
    private final CarteiraService carteiras;
    private final PessoaService pessoas;
    private final CategoriaService categorias;
    private final Clock clock;

    public CompromissoService(
            CompromissoRepository repositorio,
            PagamentosRealizados pagamentos,
            CarteiraService carteiras,
            PessoaService pessoas,
            CategoriaService categorias,
            Clock clock) {
        this.repositorio = repositorio;
        this.pagamentos = pagamentos;
        this.carteiras = carteiras;
        this.pessoas = pessoas;
        this.categorias = categorias;
        this.clock = clock;
    }

    @Transactional
    public Resultado criar(UUID contaId, Plano plano) {
        return gravar(contaId, UUID.randomUUID(), plano, null, clock.instant());
    }

    @Transactional
    public Resultado atualizar(UUID contaId, UUID id, Plano plano) {
        Compromisso atual = buscar(contaId, id);
        if (mudaAsOcorrencias(atual, plano) && temPagamento(contaId, atual)) {
            throw new ConflitoException(
                    "OCORRENCIA_JA_PAGA", "Com parcelas pagas, só descrição, pessoa e categoria podem mudar.");
        }
        return gravar(contaId, id, plano, atual.canceladoAPartirDe(), atual.criadoEm());
    }

    /** De outra conta responde igual a inexistente, para não revelar que existe. */
    @Transactional(readOnly = true)
    public Compromisso buscar(UUID contaId, UUID id) {
        return repositorio.buscar(contaId, id).orElseThrow(CompromissoService::naoEncontrado);
    }

    @Transactional(readOnly = true)
    public Page<Compromisso> listar(
            UUID contaId,
            Compromisso.Natureza natureza,
            Compromisso.Forma forma,
            UUID pessoaId,
            boolean somenteAtivos,
            Pageable paginacao) {
        return repositorio.listar(contaId, natureza, forma, pessoaId, somenteAtivos, paginacao);
    }

    @Transactional(readOnly = true)
    public Detalhe detalhar(UUID contaId, UUID id) {
        Compromisso compromisso = buscar(contaId, id);
        Map<Integer, Long> pagas = pagamentos.ocorrenciasPagas(contaId, id);
        Set<YearMonth> faturasPagas = faturasPagas(contaId, compromisso);
        List<OcorrenciaDetalhada> ocorrencias = ocorrencias(contaId, compromisso, horizonte()).stream()
                .map(o -> detalhar(o, pagas, faturasPagas))
                .toList();
        return new Detalhe(compromisso, ocorrencias);
    }

    /** Termina o plano a partir da data, mantendo o histórico do que já venceu. */
    @Transactional
    public Compromisso cancelar(UUID contaId, UUID id, LocalDate aPartirDe) {
        return repositorio.salvar(buscar(contaId, id).cancelado(aPartirDe));
    }

    @Transactional
    public void excluir(UUID contaId, UUID id) {
        Compromisso compromisso = buscar(contaId, id);
        if (temPagamento(contaId, compromisso)) {
            throw new ConflitoException("OCORRENCIA_JA_PAGA", "O compromisso já tem parcelas pagas.");
        }
        repositorio.excluir(compromisso);
    }

    @Transactional
    public OcorrenciaDetalhada editarOcorrencia(UUID contaId, UUID id, int numero, EdicaoDeOcorrencia edicao) {
        Compromisso compromisso = buscar(contaId, id);
        Ocorrencia alvo = ocorrencia(contaId, compromisso, numero);
        garantirNaoPaga(contaId, compromisso, alvo);
        if (edicao.escopo() != Escopo.SOMENTE_ESTA && temMudancaDeData(edicao)) {
            throw new RegraDeNegocioException("ESCOPO_INVALIDO", "Só o valor pode valer para mais de uma parcela.");
        }
        if (edicao.valorCentavos() != null && edicao.valorCentavos() <= 0) {
            throw new RegraDeNegocioException("VALOR_INVALIDO", "O valor precisa ser maior que zero.");
        }
        if (edicao.fatura() != null && !compromisso.noCartao()) {
            throw new RegraDeNegocioException("CARTEIRA_INCOMPATIVEL", "Só compra no cartão tem fatura.");
        }
        List<AjusteOcorrencia> ajustes = repositorio.ajustes(id);
        int numeroDoAjuste = edicao.escopo() == Escopo.TODAS ? 1 : numero;
        if (edicao.valorCentavos() != null && edicao.escopo() != Escopo.SOMENTE_ESTA) {
            limparValoresAPartirDe(id, ajustes, numeroDoAjuste);
            ajustes = repositorio.ajustes(id);
        }
        repositorio.salvarAjuste(id, mesclar(ajustes, numeroDoAjuste, edicao));
        Ocorrencia editada = ocorrencia(contaId, repositorio.buscar(contaId, id).orElseThrow(), numero);
        return new OcorrenciaDetalhada(editada, Situacao.PREVISTO, editada.valorCentavos());
    }

    /** Os ajustes de um compromisso que quem chama já leu pela conta. */
    @Transactional(readOnly = true)
    public List<AjusteOcorrencia> ajustes(UUID compromissoId) {
        return repositorio.ajustes(compromissoId);
    }

    /** A ocorrência, para quem precisa registrar um pagamento. Numero inexistente responde 404. */
    @Transactional(readOnly = true)
    public Ocorrencia ocorrencia(UUID contaId, Compromisso compromisso, int numero) {
        YearMonth ate = YearMonth.from(compromisso.dataInicio()).plusYears(numero + 1L);
        return ocorrencias(contaId, compromisso, ate).stream()
                .filter(o -> o.numero() == numero)
                .findFirst()
                .orElseThrow(CompromissoService::naoEncontrado);
    }

    /** O limite comprometido soma tudo o que falta pagar das compras e o que já foi cobrado de assinaturas. */
    @Override
    @Transactional(readOnly = true)
    public long limiteUsadoCentavos(UUID contaId, UUID cartaoId) {
        RegraDeFatura regra = carteiras.buscarCartao(contaId, cartaoId).getRegra();
        Set<YearMonth> pagas = pagamentos.faturasPagas(contaId, cartaoId);
        YearMonth faturaAtual = regra.faturaDaCompra(hoje()).competencia();
        return repositorio.listarDoCartao(contaId, cartaoId).stream()
                .flatMap(c -> GeradorDeOcorrencias.gerar(c, regra, repositorio.ajustes(c.id()), faturaAtual).stream())
                .filter(o -> !o.ignorada() && !pagas.contains(o.competencia()))
                .mapToLong(Ocorrencia::valorCentavos)
                .sum();
    }

    private Resultado gravar(UUID contaId, UUID id, Plano p, LocalDate canceladoAPartirDe, Instant criadoEm) {
        Compromisso compromisso = new Compromisso(
                id,
                contaId,
                p.natureza(),
                p.forma(),
                p.certeza(),
                p.descricao().strip(),
                p.valorCentavos(),
                p.parcelas(),
                p.dataInicio(),
                p.dataFim(),
                p.recorrenciaAnual(),
                p.carteiraId(),
                p.cartaoId(),
                p.faturaInicial(),
                p.pessoaId(),
                p.categoriaId(),
                canceladoAPartirDe,
                criadoEm);
        validarReferencias(contaId, compromisso);
        return new Resultado(repositorio.salvar(compromisso), avisos(contaId, compromisso));
    }

    private void validarReferencias(UUID contaId, Compromisso c) {
        if (c.carteiraId() != null) {
            carteiras.buscarCarteira(contaId, c.carteiraId());
        }
        if (c.cartaoId() != null) {
            carteiras.buscarCartao(contaId, c.cartaoId());
        }
        if (c.pessoaId() != null) {
            pessoas.buscar(contaId, c.pessoaId());
        }
        if (c.categoriaId() != null) {
            categorias.buscar(contaId, c.categoriaId());
        }
    }

    private boolean temPagamento(UUID contaId, Compromisso c) {
        return !pagamentos.ocorrenciasPagas(contaId, c.id()).isEmpty()
                || (c.noCartao() && temOcorrenciaEmFaturaPaga(contaId, c));
    }

    /** Mudar qualquer um destes mudaria valor, data ou numeração de parcelas que já foram pagas. */
    private static boolean mudaAsOcorrencias(Compromisso c, Plano p) {
        return c.natureza() != p.natureza()
                || c.forma() != p.forma()
                || c.valorCentavos() != p.valorCentavos()
                || !Objects.equals(c.parcelas(), p.parcelas())
                || !c.dataInicio().equals(p.dataInicio())
                || !Objects.equals(c.dataFim(), p.dataFim())
                || c.recorrenciaAnual() != p.recorrenciaAnual()
                || !Objects.equals(c.carteiraId(), p.carteiraId())
                || !Objects.equals(c.cartaoId(), p.cartaoId())
                || !Objects.equals(c.faturaInicial(), p.faturaInicial());
    }

    private List<String> avisos(UUID contaId, Compromisso c) {
        if (c.faturaInicial() == null) {
            return List.of();
        }
        YearMonth sugerida =
                carteiras.faturaSugerida(contaId, c.cartaoId(), c.dataInicio()).competencia();
        return sugerida.equals(c.faturaInicial()) ? List.of() : List.of(AVISO_FATURA);
    }

    private List<Ocorrencia> ocorrencias(UUID contaId, Compromisso c, YearMonth ate) {
        RegraDeFatura regra =
                c.noCartao() ? carteiras.buscarCartao(contaId, c.cartaoId()).getRegra() : null;
        return GeradorDeOcorrencias.gerar(c, regra, repositorio.ajustes(c.id()), ate);
    }

    private OcorrenciaDetalhada detalhar(Ocorrencia o, Map<Integer, Long> pagas, Set<YearMonth> faturasPagas) {
        Long valorPago = pagas.get(o.numero());
        if (valorPago != null) {
            return new OcorrenciaDetalhada(o, Situacao.REALIZADO, valorPago);
        }
        Situacao situacao = faturasPagas.contains(o.competencia()) ? Situacao.REALIZADO : Situacao.PREVISTO;
        return new OcorrenciaDetalhada(o, situacao, o.valorCentavos());
    }

    private Set<YearMonth> faturasPagas(UUID contaId, Compromisso c) {
        return c.noCartao() ? pagamentos.faturasPagas(contaId, c.cartaoId()) : Set.of();
    }

    private boolean temOcorrenciaEmFaturaPaga(UUID contaId, Compromisso c) {
        Set<YearMonth> pagas = faturasPagas(contaId, c);
        return ocorrencias(contaId, c, horizonte()).stream().anyMatch(o -> pagas.contains(o.competencia()));
    }

    private void garantirNaoPaga(UUID contaId, Compromisso c, Ocorrencia alvo) {
        boolean paga = pagamentos.ocorrenciasPagas(contaId, c.id()).containsKey(alvo.numero())
                || faturasPagas(contaId, c).contains(alvo.competencia());
        if (paga) {
            throw new ConflitoException("OCORRENCIA_JA_PAGA", "Essa parcela já foi paga.");
        }
    }

    private void limparValoresAPartirDe(UUID id, List<AjusteOcorrencia> ajustes, int numero) {
        ajustes.stream().filter(a -> a.numero() >= numero).forEach(a -> repositorio.salvarAjuste(id, a.semValor()));
    }

    private static AjusteOcorrencia mesclar(List<AjusteOcorrencia> ajustes, int numero, EdicaoDeOcorrencia e) {
        AjusteOcorrencia atual = ajustes.stream()
                .filter(a -> a.numero() == numero)
                .findFirst()
                .orElseGet(() -> AjusteOcorrencia.vazio(numero));
        boolean propaga = e.valorCentavos() != null ? e.escopo() != Escopo.SOMENTE_ESTA : atual.propaga();
        return new AjusteOcorrencia(
                numero,
                e.valorCentavos() != null ? e.valorCentavos() : atual.valorCentavos(),
                e.vencimento() != null ? e.vencimento() : atual.vencimento(),
                e.fatura() != null ? e.fatura() : atual.fatura(),
                e.ignorada() != null ? e.ignorada() : atual.ignorada(),
                propaga);
    }

    private static boolean temMudancaDeData(EdicaoDeOcorrencia e) {
        return e.vencimento() != null || e.fatura() != null || e.ignorada() != null;
    }

    private YearMonth horizonte() {
        return YearMonth.from(hoje()).plusMonths(MESES_A_FRENTE);
    }

    private LocalDate hoje() {
        return Calendario.hoje(clock);
    }

    private static RecursoNaoEncontradoException naoEncontrado() {
        return new RecursoNaoEncontradoException("RECURSO_NAO_ENCONTRADO", "Recurso não encontrado.");
    }

    /** Vale exigir o mesmo tipo: pagar com a carteira errada misturaria o vale com a sobra. */
    public static void garantirCarteiraCompativel(Carteira origem, Carteira pagamento) {
        boolean origemVale = origem.getTipo() == Carteira.Tipo.VALE;
        boolean pagamentoVale = pagamento.getTipo() == Carteira.Tipo.VALE;
        if (origemVale != pagamentoVale) {
            throw new RegraDeNegocioException("CARTEIRA_INCOMPATIVEL", "Vale só se move com vale.");
        }
    }
}
