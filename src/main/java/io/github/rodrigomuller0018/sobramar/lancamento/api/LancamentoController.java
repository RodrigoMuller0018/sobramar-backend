package io.github.rodrigomuller0018.sobramar.lancamento.api;

import io.github.rodrigomuller0018.sobramar.lancamento.aplicacao.LancamentoService;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.Lancamento;
import io.github.rodrigomuller0018.sobramar.lancamento.dominio.LancamentoRepository;
import io.github.rodrigomuller0018.sobramar.shared.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
class LancamentoController {

    record LancamentoRequest(
            @NotNull Lancamento.Tipo tipo,

            @NotBlank @Size(max = Lancamento.DESCRICAO_MAXIMA)
            String descricao,

            @NotNull Long valorCentavos,
            @NotNull LocalDate data,
            @NotNull UUID carteiraId,
            UUID carteiraDestinoId,
            UUID pessoaId,
            UUID categoriaId,
            Boolean ignorado) {

        LancamentoService.Dados dados() {
            return new LancamentoService.Dados(
                    tipo,
                    descricao,
                    valorCentavos,
                    data,
                    carteiraId,
                    carteiraDestinoId,
                    pessoaId,
                    categoriaId,
                    Boolean.TRUE.equals(ignorado));
        }
    }

    record PagamentoRequest(
            @NotNull LocalDate data,
            @NotNull Long valorCentavos,
            @NotNull UUID carteiraId) {

        LancamentoService.Pagamento pagamento() {
            return new LancamentoService.Pagamento(data, valorCentavos, carteiraId);
        }
    }

    record TotalDaFaturaRequest(@NotNull Long totalCentavos) {}

    record LancamentoResponse(
            UUID id,
            Lancamento.Tipo tipo,
            String descricao,
            long valorCentavos,
            LocalDate data,
            UUID carteiraId,
            UUID carteiraDestinoId,
            UUID compromissoId,
            Integer numeroOcorrencia,
            UUID pessoaId,
            UUID categoriaId,
            boolean ignorado) {
        static LancamentoResponse de(Lancamento l) {
            return new LancamentoResponse(
                    l.id(),
                    l.tipo(),
                    l.descricao(),
                    l.valorCentavos(),
                    l.data(),
                    l.carteiraId(),
                    l.carteiraDestinoId(),
                    l.compromissoId(),
                    l.numeroOcorrencia(),
                    l.pessoaId(),
                    l.categoriaId(),
                    l.ignorado());
        }
    }

    private final LancamentoService servico;

    LancamentoController(LancamentoService servico) {
        this.servico = servico;
    }

    @GetMapping("/lancamentos")
    PagedModel<LancamentoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) UUID carteiraId,
            @RequestParam(required = false) UUID pessoaId,
            @RequestParam(required = false) UUID categoriaId,
            Pageable paginacao) {
        LancamentoRepository.Filtro filtro =
                new LancamentoRepository.Filtro(de, ate, carteiraId, pessoaId, categoriaId);
        return new PagedModel<>(
                servico.listar(usuario.contaId(), filtro, paginacao).map(LancamentoResponse::de));
    }

    @PostMapping("/lancamentos")
    ResponseEntity<LancamentoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody LancamentoRequest r) {
        Lancamento lancamento = servico.criar(usuario.contaId(), r.dados());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(lancamento.id())
                .toUri();
        return ResponseEntity.created(local).body(LancamentoResponse.de(lancamento));
    }

    @GetMapping("/lancamentos/{id}")
    LancamentoResponse buscar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return LancamentoResponse.de(servico.buscar(usuario.contaId(), id));
    }

    @PutMapping("/lancamentos/{id}")
    LancamentoResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody LancamentoRequest r) {
        return LancamentoResponse.de(servico.atualizar(usuario.contaId(), id, r.dados()));
    }

    @DeleteMapping("/lancamentos/{id}")
    ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        servico.excluir(usuario.contaId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/compromissos/{id}/ocorrencias/{numero}/pagamento")
    @ResponseStatus(HttpStatus.CREATED)
    LancamentoResponse pagarOcorrencia(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @PathVariable int numero,
            @Valid @RequestBody PagamentoRequest r) {
        return LancamentoResponse.de(servico.pagarOcorrencia(usuario.contaId(), id, numero, r.pagamento()));
    }

    @DeleteMapping("/compromissos/{id}/ocorrencias/{numero}/pagamento")
    ResponseEntity<Void> desfazerPagamentoDaOcorrencia(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id, @PathVariable int numero) {
        servico.desfazerPagamentoDaOcorrencia(usuario.contaId(), id, numero);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cartoes/{id}/faturas/{competencia}/pagamento")
    ResponseEntity<Void> pagarFatura(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @PathVariable YearMonth competencia,
            @Valid @RequestBody PagamentoRequest r) {
        servico.pagarFatura(usuario.contaId(), id, competencia, r.pagamento());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/cartoes/{id}/faturas/{competencia}/pagamento")
    ResponseEntity<Void> desfazerPagamentoDeFatura(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @PathVariable YearMonth competencia) {
        servico.desfazerPagamentoDeFatura(usuario.contaId(), id, competencia);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cartoes/{id}/faturas/{competencia}/total")
    ResponseEntity<Void> informarTotalDaFatura(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @PathVariable YearMonth competencia,
            @Valid @RequestBody TotalDaFaturaRequest r) {
        servico.informarTotalDaFatura(usuario.contaId(), id, competencia, r.totalCentavos());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cartoes/{id}/faturas/{competencia}/total")
    ResponseEntity<Void> esquecerTotalDaFatura(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @PathVariable YearMonth competencia) {
        servico.esquecerTotalDaFatura(usuario.contaId(), id, competencia);
        return ResponseEntity.noContent().build();
    }
}
