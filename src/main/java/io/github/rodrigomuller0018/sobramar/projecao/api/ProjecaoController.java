package io.github.rodrigomuller0018.sobramar.projecao.api;

import io.github.rodrigomuller0018.sobramar.projecao.aplicacao.ProjecaoService;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.CalculadoraDePendencias.Pendencia;
import io.github.rodrigomuller0018.sobramar.projecao.dominio.Projecao;
import io.github.rodrigomuller0018.sobramar.shared.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ProjecaoController {

    record FaturaResponse(
            UUID cartaoId,
            String nomeCartao,
            LocalDate vencimento,
            long totalCentavos,
            boolean paga,
            Long informadoCentavos,
            Long diferencaCentavos,
            boolean conciliada) {}

    record ValeResponse(UUID carteiraId, String nome, long creditosCentavos, long gastosCentavos) {}

    record MesResponse(
            YearMonth competencia,
            long receitasCentavos,
            long compromissosCentavos,
            long sobraCentavos,
            long sobraMaximaCentavos,
            boolean contemEstimativa,
            Projecao.Confianca confianca,
            List<FaturaResponse> faturas,
            List<Projecao.Item> itens,
            List<ValeResponse> vale) {}

    record ProjecaoResponse(Instant atualizadoEm, List<MesResponse> meses) {}

    record CompraRequest(
            @NotNull Long valorTotalCentavos,
            @NotNull Integer parcelas,
            @NotNull LocalDate dataInicio,
            UUID cartaoId,
            UUID carteiraId) {}

    record SimulacaoRequest(
            @NotNull YearMonth de,
            @NotNull YearMonth ate,
            @NotNull @Valid CompraRequest compra) {}

    record MesSimuladoResponse(
            YearMonth competencia, long sobraAtualCentavos, long sobraSimuladaCentavos, long diferencaCentavos) {}

    record SimulacaoResponse(
            List<MesSimuladoResponse> meses,
            long valorParcelaCentavos,
            LocalDate primeiroPagamento,
            LocalDate ultimoPagamento) {}

    record PendenciasResponse(List<Pendencia> itens) {}

    private final ProjecaoService servico;

    ProjecaoController(ProjecaoService servico) {
        this.servico = servico;
    }

    @GetMapping("/projecao")
    ProjecaoResponse projetar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam YearMonth de,
            @RequestParam YearMonth ate,
            @RequestParam(required = false) UUID pessoaId,
            @RequestParam(required = false) UUID categoriaId) {
        ProjecaoService.Resultado resultado = servico.projetar(usuario.contaId(), de, ate, pessoaId, categoriaId);
        return new ProjecaoResponse(
                resultado.atualizadoEm(),
                resultado.meses().stream().map(mes -> resposta(mes, resultado)).toList());
    }

    @PostMapping("/projecao/simulacao")
    SimulacaoResponse simular(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody SimulacaoRequest r) {
        CompraRequest c = r.compra();
        ProjecaoService.Simulacao simulacao = servico.simular(
                usuario.contaId(),
                r.de(),
                r.ate(),
                new ProjecaoService.Compra(
                        c.valorTotalCentavos(), c.parcelas(), c.dataInicio(), c.cartaoId(), c.carteiraId()));
        return new SimulacaoResponse(
                simulacao.meses().stream()
                        .map(m -> new MesSimuladoResponse(
                                m.competencia(),
                                m.sobraAtualCentavos(),
                                m.sobraSimuladaCentavos(),
                                m.sobraSimuladaCentavos() - m.sobraAtualCentavos()))
                        .toList(),
                simulacao.valorParcelaCentavos(),
                simulacao.primeiroPagamento(),
                simulacao.ultimoPagamento());
    }

    @GetMapping("/pendencias")
    PendenciasResponse pendencias(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @RequestParam(defaultValue = "7") int dias) {
        return new PendenciasResponse(servico.pendencias(usuario.contaId(), dias));
    }

    private static MesResponse resposta(Projecao.Mes mes, ProjecaoService.Resultado r) {
        return new MesResponse(
                mes.competencia(),
                mes.receitasCentavos(),
                mes.compromissosCentavos(),
                mes.sobraCentavos(),
                mes.sobraMaximaCentavos(),
                mes.contemEstimativa(),
                mes.confianca(),
                mes.faturas().stream()
                        .map(f -> new FaturaResponse(
                                f.cartaoId(),
                                r.nomesDosCartoes().get(f.cartaoId()),
                                f.vencimento(),
                                f.totalCentavos(),
                                f.paga(),
                                f.informadoCentavos(),
                                f.diferencaCentavos(),
                                f.conciliada()))
                        .toList(),
                mes.itens(),
                mes.vale().stream()
                        .map(v -> new ValeResponse(
                                v.carteiraId(),
                                r.nomesDasCarteiras().get(v.carteiraId()),
                                v.creditosCentavos(),
                                v.gastosCentavos()))
                        .toList());
    }
}
