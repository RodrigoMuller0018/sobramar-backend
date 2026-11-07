package io.github.rodrigomuller0018.sobramar.compromisso.api;

import io.github.rodrigomuller0018.sobramar.compromisso.aplicacao.CompromissoService;
import io.github.rodrigomuller0018.sobramar.compromisso.aplicacao.CompromissoService.EdicaoDeOcorrencia;
import io.github.rodrigomuller0018.sobramar.compromisso.aplicacao.CompromissoService.Escopo;
import io.github.rodrigomuller0018.sobramar.compromisso.aplicacao.CompromissoService.OcorrenciaDetalhada;
import io.github.rodrigomuller0018.sobramar.compromisso.aplicacao.CompromissoService.Plano;
import io.github.rodrigomuller0018.sobramar.compromisso.aplicacao.CompromissoService.Resultado;
import io.github.rodrigomuller0018.sobramar.compromisso.aplicacao.CompromissoService.Situacao;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso;
import io.github.rodrigomuller0018.sobramar.shared.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/compromissos")
class CompromissoController {

    /** Em PARCELADO, usa {@code valorTotalCentavos}; nas demais formas, {@code valorCentavos}. */
    record CompromissoRequest(
            @NotNull Compromisso.Natureza natureza,
            @NotNull Compromisso.Forma forma,
            @NotNull Compromisso.Certeza certeza,

            @NotBlank @Size(max = Compromisso.DESCRICAO_MAXIMA)
            String descricao,

            Long valorCentavos,
            Long valorTotalCentavos,
            Integer parcelas,
            @NotNull LocalDate dataInicio,
            LocalDate dataFim,
            Boolean recorrenciaAnual,
            UUID carteiraId,
            UUID cartaoId,
            YearMonth faturaInicial,
            UUID pessoaId,
            UUID categoriaId,
            Integer parte,
            Long valorMaximoCentavos) {

        Plano plano() {
            long valor = forma == Compromisso.Forma.PARCELADO
                    ? (valorTotalCentavos == null ? 0 : valorTotalCentavos)
                    : (valorCentavos == null ? 0 : valorCentavos);
            return new Plano(
                    natureza,
                    forma,
                    certeza,
                    descricao,
                    valor,
                    parcelas,
                    dataInicio,
                    dataFim,
                    Boolean.TRUE.equals(recorrenciaAnual),
                    carteiraId,
                    cartaoId,
                    faturaInicial,
                    pessoaId,
                    categoriaId,
                    parte,
                    valorMaximoCentavos);
        }
    }

    record CancelamentoRequest(@NotNull LocalDate aPartirDe) {}

    record EdicaoRequest(
            @NotNull Escopo escopo, Long valorCentavos, LocalDate vencimento, YearMonth fatura, Boolean ignorada) {}

    record Aviso(String codigo) {}

    record OcorrenciaResponse(
            int numero,
            long valorCentavos,
            LocalDate dataCompra,
            LocalDate vencimento,
            YearMonth competencia,
            boolean ignorada,
            Situacao situacao) {
        static OcorrenciaResponse de(OcorrenciaDetalhada d) {
            return new OcorrenciaResponse(
                    d.ocorrencia().numero(),
                    d.valorCentavos(),
                    d.ocorrencia().dataCompra(),
                    d.ocorrencia().vencimento(),
                    d.ocorrencia().competencia(),
                    d.ocorrencia().ignorada(),
                    d.situacao());
        }
    }

    record CompromissoResponse(
            UUID id,
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
            UUID categoriaId,
            Integer parte,
            Long valorMaximoCentavos,
            LocalDate canceladoAPartirDe,
            List<Aviso> avisos,
            List<OcorrenciaResponse> ocorrencias) {

        static CompromissoResponse de(Compromisso c, List<String> avisos, List<OcorrenciaResponse> ocorrencias) {
            return new CompromissoResponse(
                    c.id(),
                    c.natureza(),
                    c.forma(),
                    c.certeza(),
                    c.descricao(),
                    c.valorCentavos(),
                    c.parcelas(),
                    c.dataInicio(),
                    c.dataFim(),
                    c.recorrenciaAnual(),
                    c.carteiraId(),
                    c.cartaoId(),
                    c.faturaInicial(),
                    c.pessoaId(),
                    c.categoriaId(),
                    c.parte(),
                    c.valorMaximoCentavos(),
                    c.canceladoAPartirDe(),
                    avisos.stream().map(Aviso::new).toList(),
                    ocorrencias);
        }
    }

    private final CompromissoService servico;

    CompromissoController(CompromissoService servico) {
        this.servico = servico;
    }

    @GetMapping
    PagedModel<CompromissoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Compromisso.Natureza natureza,
            @RequestParam(required = false) Compromisso.Forma forma,
            @RequestParam(required = false) UUID pessoaId,
            @RequestParam(defaultValue = "true") boolean ativos,
            Pageable paginacao) {
        return new PagedModel<>(servico.listar(usuario.contaId(), natureza, forma, pessoaId, ativos, paginacao)
                .map(c -> CompromissoResponse.de(c, List.of(), List.of())));
    }

    @PostMapping
    ResponseEntity<CompromissoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody CompromissoRequest requisicao) {
        Resultado resultado = servico.criar(usuario.contaId(), requisicao.plano());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resultado.compromisso().id())
                .toUri();
        return ResponseEntity.created(local)
                .body(CompromissoResponse.de(resultado.compromisso(), resultado.avisos(), List.of()));
    }

    @GetMapping("/{id}")
    CompromissoResponse buscar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        CompromissoService.Detalhe detalhe = servico.detalhar(usuario.contaId(), id);
        return CompromissoResponse.de(
                detalhe.compromisso(),
                List.of(),
                detalhe.ocorrencias().stream().map(OcorrenciaResponse::de).toList());
    }

    @PutMapping("/{id}")
    CompromissoResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody CompromissoRequest requisicao) {
        Resultado resultado = servico.atualizar(usuario.contaId(), id, requisicao.plano());
        return CompromissoResponse.de(resultado.compromisso(), resultado.avisos(), List.of());
    }

    @PostMapping("/{id}/cancelamento")
    CompromissoResponse cancelar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody CancelamentoRequest requisicao) {
        return CompromissoResponse.de(
                servico.cancelar(usuario.contaId(), id, requisicao.aPartirDe()), List.of(), List.of());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        servico.excluir(usuario.contaId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ocorrencias/{numero}")
    OcorrenciaResponse editarOcorrencia(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @PathVariable int numero,
            @Valid @RequestBody EdicaoRequest r) {
        return OcorrenciaResponse.de(servico.editarOcorrencia(
                usuario.contaId(),
                id,
                numero,
                new EdicaoDeOcorrencia(r.escopo(), r.valorCentavos(), r.vencimento(), r.fatura(), r.ignorada())));
    }
}
