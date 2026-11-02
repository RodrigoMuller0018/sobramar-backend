package io.github.rodrigomuller0018.sobramar.categoria.api;

import io.github.rodrigomuller0018.sobramar.categoria.aplicacao.CategoriaService;
import io.github.rodrigomuller0018.sobramar.categoria.dominio.Categoria;
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
@RequestMapping("/api/v1/categorias")
class CategoriaController {

    record CategoriaRequest(
            @NotBlank @Size(max = Categoria.NOME_MAXIMO) String nome) {}

    record CategoriaResponse(UUID id, String nome, boolean arquivada, Instant criadoEm) {
        static CategoriaResponse de(Categoria c) {
            return new CategoriaResponse(c.getId(), c.getNome(), c.isArquivada(), c.getCriadoEm());
        }
    }

    private final CategoriaService servico;

    CategoriaController(CategoriaService servico) {
        this.servico = servico;
    }

    @GetMapping
    PagedModel<CategoriaResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "false") boolean arquivadas,
            Pageable paginacao) {
        return new PagedModel<>(
                servico.listar(usuario.contaId(), arquivadas, paginacao).map(CategoriaResponse::de));
    }

    @PostMapping
    ResponseEntity<CategoriaResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody CategoriaRequest requisicao) {
        Categoria categoria = servico.criar(usuario.contaId(), requisicao.nome());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(categoria.getId())
                .toUri();
        return ResponseEntity.created(local).body(CategoriaResponse.de(categoria));
    }

    @GetMapping("/{id}")
    CategoriaResponse buscar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        return CategoriaResponse.de(servico.buscar(usuario.contaId(), id));
    }

    @PutMapping("/{id}")
    CategoriaResponse renomear(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody CategoriaRequest requisicao) {
        return CategoriaResponse.de(servico.renomear(usuario.contaId(), id, requisicao.nome()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> arquivar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        servico.arquivar(usuario.contaId(), id);
        return ResponseEntity.noContent().build();
    }
}
