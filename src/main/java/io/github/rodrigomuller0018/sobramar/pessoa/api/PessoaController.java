package io.github.rodrigomuller0018.sobramar.pessoa.api;

import io.github.rodrigomuller0018.sobramar.pessoa.aplicacao.PessoaService;
import io.github.rodrigomuller0018.sobramar.pessoa.dominio.Pessoa;
import io.github.rodrigomuller0018.sobramar.shared.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/v1/pessoas")
class PessoaController {

    record PessoaRequest(
            @NotBlank @Size(max = Pessoa.APELIDO_MAXIMO) String apelido) {}

    record PessoaResponse(UUID id, String apelido, boolean arquivada, Instant criadoEm) {
        static PessoaResponse de(Pessoa p) {
            return new PessoaResponse(p.getId(), p.getApelido(), p.isArquivada(), p.getCriadoEm());
        }
    }

    private final PessoaService servico;

    PessoaController(PessoaService servico) {
        this.servico = servico;
    }

    @GetMapping
    PagedModel<PessoaResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "false") boolean arquivadas,
            Pageable paginacao) {
        return new PagedModel<>(
                servico.listar(usuario.contaId(), arquivadas, paginacao).map(PessoaResponse::de));
    }

    @PostMapping
    ResponseEntity<PessoaResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody PessoaRequest requisicao) {
        Pessoa pessoa = servico.criar(usuario.contaId(), requisicao.apelido());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(pessoa.getId())
                .toUri();
        return ResponseEntity.created(local).body(PessoaResponse.de(pessoa));
    }

    @GetMapping("/{id}")
    PessoaResponse buscar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return PessoaResponse.de(servico.buscar(usuario.contaId(), id));
    }

    @PutMapping("/{id}")
    PessoaResponse renomear(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody PessoaRequest requisicao) {
        return PessoaResponse.de(servico.renomear(usuario.contaId(), id, requisicao.apelido()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> arquivar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        servico.arquivar(usuario.contaId(), id);
        return ResponseEntity.noContent().build();
    }
}
