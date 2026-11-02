package io.github.rodrigomuller0018.template.tarefa.api;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rodrigomuller0018.template.shared.erro.RecursoNaoEncontradoException;
import io.github.rodrigomuller0018.template.shared.erro.RegraDeNegocioException;
import io.github.rodrigomuller0018.template.tarefa.aplicacao.TarefaService;
import io.github.rodrigomuller0018.template.tarefa.dominio.Tarefa;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Testa o contrato HTTP: status codes, Location, validação e formato de erro (Problem Details). */
@WebMvcTest(TarefaController.class)
class TarefaControllerTest {

    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TarefaService servico;

    @Test
    void deveCriarERetornar201ComLocation() throws Exception {
        Tarefa tarefa = Tarefa.restaurar(UUID.randomUUID(), "Nova", AGORA, null);
        when(servico.criar("Nova")).thenReturn(tarefa);

        mvc.perform(post("/api/v1/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Nova\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/tarefas/" + tarefa.getId())))
                .andExpect(jsonPath("$.titulo").value("Nova"))
                .andExpect(jsonPath("$.concluida").value(false));
    }

    @Test
    void deveRetornar400ComProblemDetailsQuandoTituloEmBranco() throws Exception {
        mvc.perform(post("/api/v1/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"));

        verifyNoInteractions(servico);
    }

    @Test
    void deveRetornar404ComCodigoQuandoNaoEncontrada() throws Exception {
        UUID id = UUID.randomUUID();
        when(servico.buscar(id))
                .thenThrow(new RecursoNaoEncontradoException("TAREFA_NAO_ENCONTRADA", "Tarefa não encontrada."));

        mvc.perform(get("/api/v1/tarefas/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TAREFA_NAO_ENCONTRADA"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void deveRetornar422QuandoRegraDeNegocioViolada() throws Exception {
        UUID id = UUID.randomUUID();
        when(servico.concluir(id))
                .thenThrow(new RegraDeNegocioException("TAREFA_JA_CONCLUIDA", "A tarefa já foi concluída."));

        mvc.perform(patch("/api/v1/tarefas/{id}/conclusao", id))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("TAREFA_JA_CONCLUIDA"));
    }
}
