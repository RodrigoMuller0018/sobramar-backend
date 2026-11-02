package io.github.rodrigomuller0018.template.tarefa.api;

import io.github.rodrigomuller0018.template.tarefa.aplicacao.TarefaService;
import io.github.rodrigomuller0018.template.tarefa.dominio.Tarefa;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/tarefas")
class TarefaController {

    private final TarefaService servico;

    TarefaController(TarefaService servico) {
        this.servico = servico;
    }

    @PostMapping
    ResponseEntity<TarefaResponse> criar(@Valid @RequestBody CriarTarefaRequest requisicao) {
        Tarefa tarefa = servico.criar(requisicao.titulo());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(tarefa.getId())
                .toUri();
        return ResponseEntity.created(local).body(TarefaResponse.de(tarefa));
    }

    @GetMapping("/{id}")
    TarefaResponse buscar(@PathVariable UUID id) {
        return TarefaResponse.de(servico.buscar(id));
    }

    @GetMapping
    PagedModel<TarefaResponse> listar(Pageable paginacao) {
        return new PagedModel<>(servico.listar(paginacao).map(TarefaResponse::de));
    }

    @PatchMapping("/{id}/conclusao")
    TarefaResponse concluir(@PathVariable UUID id) {
        return TarefaResponse.de(servico.concluir(id));
    }
}
