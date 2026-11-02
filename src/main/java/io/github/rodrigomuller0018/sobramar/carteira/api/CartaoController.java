package io.github.rodrigomuller0018.sobramar.carteira.api;

import io.github.rodrigomuller0018.sobramar.carteira.aplicacao.CarteiraService;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.Cartao;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.shared.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/cartoes")
class CartaoController {

    record CartaoRequest(
            @NotBlank @Size(max = Cartao.NOME_MAXIMO) String nome,
            @NotNull Integer diaFechamento,
            @NotNull Integer diaVencimento,
            Long limiteCentavos) {

        RegraDeFatura regra() {
            return new RegraDeFatura(diaFechamento, diaVencimento);
        }
    }

    record CartaoResponse(
            UUID id,
            String nome,
            int diaFechamento,
            int diaVencimento,
            Long limiteCentavos,
            boolean arquivado,
            Instant criadoEm) {
        static CartaoResponse de(Cartao c) {
            return new CartaoResponse(
                    c.getId(),
                    c.getNome(),
                    c.getRegra().diaFechamento(),
                    c.getRegra().diaVencimento(),
                    c.getLimiteCentavos(),
                    c.isArquivado(),
                    c.getCriadoEm());
        }
    }

    record FaturaSugeridaResponse(String competencia, LocalDate fechamento, LocalDate vencimento) {}

    private final CarteiraService servico;

    CartaoController(CarteiraService servico) {
        this.servico = servico;
    }

    @GetMapping
    PagedModel<CartaoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "false") boolean arquivadas,
            Pageable paginacao) {
        return new PagedModel<>(
                servico.listarCartoes(usuario.contaId(), arquivadas, paginacao).map(CartaoResponse::de));
    }

    @PostMapping
    ResponseEntity<CartaoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody CartaoRequest r) {
        Cartao cartao = servico.criarCartao(usuario.contaId(), r.nome(), r.regra(), r.limiteCentavos());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(cartao.getId())
                .toUri();
        return ResponseEntity.created(local).body(CartaoResponse.de(cartao));
    }

    @GetMapping("/{id}")
    CartaoResponse buscar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return CartaoResponse.de(servico.buscarCartao(usuario.contaId(), id));
    }

    @PutMapping("/{id}")
    CartaoResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody CartaoRequest r) {
        return CartaoResponse.de(
                servico.atualizarCartao(usuario.contaId(), id, r.nome(), r.regra(), r.limiteCentavos()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> arquivar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        servico.arquivarCartao(usuario.contaId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/fatura-sugerida")
    FaturaSugeridaResponse faturaSugerida(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCompra) {
        RegraDeFatura.Fatura fatura = servico.faturaSugerida(usuario.contaId(), id, dataCompra);
        return new FaturaSugeridaResponse(fatura.competencia().toString(), fatura.fechamento(), fatura.vencimento());
    }
}
