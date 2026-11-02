package io.github.rodrigomuller0018.template.tarefa.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.rodrigomuller0018.template.shared.erro.RecursoNaoEncontradoException;
import io.github.rodrigomuller0018.template.tarefa.dominio.Tarefa;
import io.github.rodrigomuller0018.template.tarefa.dominio.TarefaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");

    @Mock
    private TarefaRepository repositorio;

    @Captor
    private ArgumentCaptor<Tarefa> tarefaSalva;

    private TarefaService servico;

    @BeforeEach
    void configurar() {
        servico = new TarefaService(repositorio, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Nested
    class Criar {

        @Test
        void deveSalvarTarefaNovaComDataDoRelogio() {
            when(repositorio.salvar(tarefaSalva.capture())).thenAnswer(inv -> inv.getArgument(0));

            Tarefa criada = servico.criar("Configurar pipeline");

            assertThat(tarefaSalva.getValue().getTitulo()).isEqualTo("Configurar pipeline");
            assertThat(tarefaSalva.getValue().getCriadaEm()).isEqualTo(AGORA);
            assertThat(criada).isSameAs(tarefaSalva.getValue());
        }
    }

    @Nested
    class Buscar {

        @Test
        void deveDevolverTarefaExistente() {
            Tarefa existente = Tarefa.restaurar(UUID.randomUUID(), "Existente", AGORA, null);
            when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

            assertThat(servico.buscar(existente.getId())).isSameAs(existente);
        }

        @Test
        void deveLancarNaoEncontradoQuandoNaoExiste() {
            UUID id = UUID.randomUUID();
            when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servico.buscar(id))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .extracting("codigo")
                    .isEqualTo("TAREFA_NAO_ENCONTRADA");
        }
    }

    @Nested
    class Listar {

        @Test
        void deveRepassarPaginacaoAoRepositorio() {
            PageRequest paginacao = PageRequest.of(1, 10);
            Tarefa tarefa = Tarefa.restaurar(UUID.randomUUID(), "Item", AGORA, null);
            when(repositorio.listar(paginacao)).thenReturn(new PageImpl<>(List.of(tarefa), paginacao, 11));

            assertThat(servico.listar(paginacao).getContent()).containsExactly(tarefa);
        }
    }

    @Nested
    class Concluir {

        @Test
        void deveConcluirESalvar() {
            Tarefa aberta = Tarefa.restaurar(UUID.randomUUID(), "Aberta", AGORA, null);
            when(repositorio.buscarPorId(aberta.getId())).thenReturn(Optional.of(aberta));
            when(repositorio.salvar(aberta)).thenReturn(aberta);

            Tarefa concluida = servico.concluir(aberta.getId());

            assertThat(concluida.isConcluida()).isTrue();
            verify(repositorio).salvar(argThat(Tarefa::isConcluida));
        }

        @Test
        void naoDeveSalvarQuandoTarefaNaoExiste() {
            UUID id = UUID.randomUUID();
            when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servico.concluir(id)).isInstanceOf(RecursoNaoEncontradoException.class);
            verify(repositorio, never()).salvar(argThat(t -> true));
        }
    }
}
