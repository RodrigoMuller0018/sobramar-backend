package io.github.rodrigomuller0018.sobramar.carteira.api;

import io.github.rodrigomuller0018.sobramar.carteira.aplicacao.CarteiraService;
import io.github.rodrigomuller0018.sobramar.carteira.dominio.Carteira;
import io.github.rodrigomuller0018.sobramar.shared.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
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
@RequestMapping("/api/v1/carteiras")
class CarteiraController {

    record CarteiraRequest(
            @NotNull Carteira.Tipo tipo,
            @NotBlank @Size(max = Carteira.NOME_MAXIMO) String nome,
            @NotNull Long saldoInicialCentavos) {}

    record CarteiraResponse(
            UUID id, Carteira.Tipo tipo, String nome, long saldoInicialCentavos, boolean arquivada, Instant criadoEm) {
        static CarteiraResponse de(Carteira c) {
            return new CarteiraResponse(
                    c.getId(), c.getTipo(), c.getNome(), c.getSaldoInicialCentavos(), c.isArquivada(), c.getCriadoEm());
        }
    }

    private final CarteiraService servico;

    CarteiraController(CarteiraService servico) {
        this.servico = servico;
    }

    @GetMapping
    PagedModel<CarteiraResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "false") boolean arquivadas,
            Pageable paginacao) {
        return new PagedModel<>(servico.listarCarteiras(usuario.contaId(), arquivadas, paginacao)
                .map(CarteiraResponse::de));
    }

    @PostMapping
    ResponseEntity<CarteiraResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody CarteiraRequest r) {
        Carteira carteira = servico.criarCarteira(usuario.contaId(), r.tipo(), r.nome(), r.saldoInicialCentavos());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(carteira.getId())
                .toUri();
        return ResponseEntity.created(local).body(CarteiraResponse.de(carteira));
    }

    @GetMapping("/{id}")
    CarteiraResponse buscar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return CarteiraResponse.de(servico.buscarCarteira(usuario.contaId(), id));
    }

    @PutMapping("/{id}")
    CarteiraResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody CarteiraRequest r) {
        return CarteiraResponse.de(
                servico.atualizarCarteira(usuario.contaId(), id, r.tipo(), r.nome(), r.saldoInicialCentavos()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> arquivar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        servico.arquivarCarteira(usuario.contaId(), id);
        return ResponseEntity.noContent().build();
    }
}
