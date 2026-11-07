package io.github.rodrigomuller0018.sobramar.projecao.aplicacao;

import io.github.rodrigomuller0018.sobramar.carteira.aplicacao.CarteiraService;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.Cartao;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.Carteira;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.compromisso.aplicacao.CompromissoService;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.GeradorDeOcorrencias;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Ocorrencia;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.PagamentosRealizados;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.PagamentosRealizados.FaturaPaga;
import io.github.rodrigomuller0018.sobramar.lancamento.aplicacao.LancamentoService;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.Lancamento;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.LancamentoRepository;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDePendencias;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDePendencias.Pendencia;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDeProjecao;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDeProjecao.CompromissoComAjustes;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDeProjecao.Entrada;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDeProjecao.Filtro;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao;
import io.github.rodrigomuller0018.sobramar.shared.dominio.Calendario;
import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta os dados da conta e chama a calculadora, que é pura. */
@Service
public class ProjecaoService {

    private static final int MESES_MAXIMOS = 24;
    private static final int DIAS_MAXIMOS = 31;

    /** @param atualizadoEm quando os dados da conta mudaram pela última vez; nulo em conta vazia */
    public record Resultado(
            List<Projecao.Mes> meses,
            Map<UUID, String> nomesDosCartoes,
            Map<UUID, String> nomesDasCarteiras,
            Instant atualizadoEm) {}

    public record Compra(long valorTotalCentavos, int parcelas, LocalDate dataInicio, UUID cartaoId, UUID carteiraId) {}

    public record MesSimulado(YearMonth competencia, long sobraAtualCentavos, long sobraSimuladaCentavos) {}

    public record Simulacao(
            List<MesSimulado> meses,
            long valorParcelaCentavos,
            LocalDate primeiroPagamento,
            LocalDate ultimoPagamento) {}

    private record Dados(
            Entrada entrada, Map<UUID, String> cartoes, Map<UUID, String> carteiras, Instant atualizadoEm) {}

    private final CompromissoService compromissos;
    private final LancamentoService lancamentos;
    private final CarteiraService carteiras;
    private final PagamentosRealizados pagamentos;
    private final Clock clock;

    public ProjecaoService(
            CompromissoService compromissos,
            LancamentoService lancamentos,
            CarteiraService carteiras,
            PagamentosRealizados pagamentos,
            Clock clock) {
        this.compromissos = compromissos;
        this.lancamentos = lancamentos;
        this.carteiras = carteiras;
        this.pagamentos = pagamentos;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Resultado projetar(UUID contaId, YearMonth de, YearMonth ate, UUID pessoaId, UUID categoriaId) {
        validarPeriodo(de, ate);
        Dados dados = dados(contaId);
        return new Resultado(
                CalculadoraDeProjecao.calcular(dados.entrada(), de, ate, new Filtro(pessoaId, categoriaId)),
                dados.cartoes(),
                dados.carteiras(),
                dados.atualizadoEm());
    }

    /** Mesma conta da projeção, com uma compra hipotética. Nada é gravado. */
    @Transactional(readOnly = true)
    public Simulacao simular(UUID contaId, YearMonth de, YearMonth ate, Compra compra) {
        validarPeriodo(de, ate);
        Compromisso hipotetico = hipotetico(contaId, compra);
        Dados dados = dados(contaId);
        List<CompromissoComAjustes> comACompra = new ArrayList<>(dados.entrada().compromissos());
        comACompra.add(new CompromissoComAjustes(hipotetico, List.of()));
        Entrada simulada = new Entrada(
                comACompra,
                dados.entrada().lancamentos(),
                dados.entrada().regrasDosCartoes(),
                dados.entrada().faturasPagas(),
                dados.entrada().faturasInformadas(),
                dados.entrada().carteirasDeVale());

        List<Projecao.Mes> atual = CalculadoraDeProjecao.calcular(dados.entrada(), de, ate, Filtro.NENHUM);
        List<Projecao.Mes> simulado = CalculadoraDeProjecao.calcular(simulada, de, ate, Filtro.NENHUM);
        List<MesSimulado> meses = new ArrayList<>();
        for (int i = 0; i < atual.size(); i++) {
            meses.add(new MesSimulado(
                    atual.get(i).competencia(),
                    atual.get(i).sobraCentavos(),
                    simulado.get(i).sobraCentavos()));
        }

        RegraDeFatura regra =
                hipotetico.noCartao() ? dados.entrada().regrasDosCartoes().get(hipotetico.cartaoId()) : null;
        List<Ocorrencia> parcelas = GeradorDeOcorrencias.gerar(hipotetico, regra, List.of(), ate);
        return new Simulacao(
                meses,
                parcelas.getFirst().valorCentavos(),
                parcelas.getFirst().vencimento(),
                parcelas.getLast().vencimento());
    }

    @Transactional(readOnly = true)
    public List<Pendencia> pendencias(UUID contaId, int dias) {
        if (dias < 0 || dias > DIAS_MAXIMOS) {
            throw new RegraDeNegocioException("PERIODO_INVALIDO", "Use de 0 a 31 dias.");
        }
        Dados dados = dados(contaId);
        return CalculadoraDePendencias.calcular(dados.entrada(), Calendario.hoje(clock), dias, dados.cartoes());
    }

    private Compromisso hipotetico(UUID contaId, Compra c) {
        if (c.cartaoId() != null) {
            carteiras.buscarCartao(contaId, c.cartaoId());
        }
        if (c.carteiraId() != null) {
            carteiras.buscarCarteira(contaId, c.carteiraId());
        }
        return new Compromisso(
                UUID.randomUUID(),
                contaId,
                Compromisso.Natureza.SAIDA,
                Compromisso.Forma.PARCELADO,
                Compromisso.Certeza.CERTO,
                "Simulação",
                c.valorTotalCentavos(),
                c.parcelas(),
                c.dataInicio(),
                null,
                false,
                c.carteiraId(),
                c.cartaoId(),
                null,
                null,
                null,
                null,
                null,
                clock.instant());
    }

    private Dados dados(UUID contaId) {
        List<Cartao> cartoes =
                carteiras.listarCartoes(contaId, true, Pageable.unpaged()).getContent();
        List<Carteira> todasCarteiras =
                carteiras.listarCarteiras(contaId, true, Pageable.unpaged()).getContent();
        List<CompromissoComAjustes> planos =
                compromissos.listar(contaId, null, null, null, false, Pageable.unpaged()).stream()
                        .map(c -> new CompromissoComAjustes(c, compromissos.ajustes(c.id())))
                        .toList();
        List<Lancamento> todosLancamentos = lancamentos
                .listar(contaId, new LancamentoRepository.Filtro(null, null, null, null, null), Pageable.unpaged())
                .getContent();
        Map<UUID, RegraDeFatura> regras = cartoes.stream().collect(Collectors.toMap(Cartao::getId, Cartao::getRegra));
        Map<UUID, List<FaturaPaga>> pagas = cartoes.stream()
                .collect(Collectors.toMap(Cartao::getId, c -> pagamentos.faturasPagasDetalhadas(contaId, c.getId())));
        Map<UUID, Set<YearMonth>> faturasPagas = new HashMap<>();
        Map<UUID, Map<YearMonth, Long>> faturasInformadas = new HashMap<>();
        pagas.forEach((cartao, lista) -> {
            faturasPagas.put(cartao, lista.stream().map(FaturaPaga::competencia).collect(Collectors.toSet()));
            faturasInformadas.put(
                    cartao,
                    lista.stream().collect(Collectors.toMap(FaturaPaga::competencia, FaturaPaga::valorCentavos)));
        });
        Set<UUID> vales = todasCarteiras.stream()
                .filter(c -> c.getTipo() == Carteira.Tipo.VALE)
                .map(Carteira::getId)
                .collect(Collectors.toSet());
        return new Dados(
                new Entrada(planos, todosLancamentos, regras, faturasPagas, faturasInformadas, vales),
                cartoes.stream().collect(Collectors.toMap(Cartao::getId, Cartao::getNome)),
                todasCarteiras.stream().collect(Collectors.toMap(Carteira::getId, Carteira::getNome)),
                atualizadoEm(planos, todosLancamentos, pagas));
    }

    /** Edição de compromisso mantém o {@code criadoEm}; o que bumpa a data é lançar, dar baixa e pagar fatura. */
    private static Instant atualizadoEm(
            List<CompromissoComAjustes> planos, List<Lancamento> lancamentos, Map<UUID, List<FaturaPaga>> pagas) {
        List<Instant> instantes = new ArrayList<>();
        planos.forEach(c -> instantes.add(c.compromisso().criadoEm()));
        lancamentos.forEach(l -> instantes.add(l.criadoEm()));
        pagas.values().forEach(lista -> lista.forEach(f -> instantes.add(f.pagaEm())));
        return Projecao.ultimaAtualizacao(instantes).orElse(null);
    }

    private static void validarPeriodo(YearMonth de, YearMonth ate) {
        long meses = ChronoUnit.MONTHS.between(de, ate) + 1;
        if (meses < 1 || meses > MESES_MAXIMOS) {
            throw new RegraDeNegocioException("PERIODO_INVALIDO", "Escolha um período de 1 a 24 meses.");
        }
    }
}
