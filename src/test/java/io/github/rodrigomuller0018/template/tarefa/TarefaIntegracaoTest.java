package io.github.rodrigomuller0018.template.tarefa;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rodrigomuller0018.template.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fluxo completo contra um PostgreSQL real: valida migrações Flyway, mapeamento JPA
 * (ddl-auto=validate) e o contrato HTTP de ponta a ponta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TarefaIntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void deveCriarBuscarListarEConcluirTarefa() throws Exception {
        String location = mvc.perform(post("/api/v1/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Fluxo completo\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Fluxo completo"));

        mvc.perform(get("/api/v1/tarefas").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));

        mvc.perform(patch(location + "/conclusao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concluida").value(true));

        mvc.perform(patch(location + "/conclusao"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("TAREFA_JA_CONCLUIDA"));
    }

    @Test
    void deveRetornar404ParaTarefaInexistente() throws Exception {
        mvc.perform(get("/api/v1/tarefas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TAREFA_NAO_ENCONTRADA"));
    }
}
