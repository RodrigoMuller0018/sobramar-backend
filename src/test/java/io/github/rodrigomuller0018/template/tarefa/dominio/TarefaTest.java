package io.github.rodrigomuller0018.template.tarefa.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.rodrigomuller0018.template.shared.erro.RegraDeNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TarefaTest {

    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");
    private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);

    @Nested
    class Nova {

        @Test
        void deveCriarTarefaAbertaComTituloSemEspacosNasPontas() {
            Tarefa tarefa = Tarefa.nova("  Estudar Flyway  ", RELOGIO);

            assertThat(tarefa.getId()).isNotNull();
            assertThat(tarefa.getTitulo()).isEqualTo("Estudar Flyway");
            assertThat(tarefa.getCriadaEm()).isEqualTo(AGORA);
            assertThat(tarefa.isConcluida()).isFalse();
            assertThat(tarefa.getConcluidaEm()).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void deveRecusarTituloVazio(String titulo) {
            assertThatThrownBy(() -> Tarefa.nova(titulo, RELOGIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .extracting("codigo")
                    .isEqualTo("TITULO_OBRIGATORIO");
        }

        @Test
        void deveAceitarTituloNoLimiteDeTamanho() {
            String titulo = "a".repeat(Tarefa.TAMANHO_MAXIMO_TITULO);

            assertThat(Tarefa.nova(titulo, RELOGIO).getTitulo()).hasSize(Tarefa.TAMANHO_MAXIMO_TITULO);
        }

        @Test
        void deveRecusarTituloAcimaDoLimite() {
            String titulo = "a".repeat(Tarefa.TAMANHO_MAXIMO_TITULO + 1);

            assertThatThrownBy(() -> Tarefa.nova(titulo, RELOGIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .extracting("codigo")
                    .isEqualTo("TITULO_MUITO_LONGO");
        }
    }

    @Nested
    class Concluir {

        @Test
        void deveRegistrarMomentoDaConclusao() {
            Tarefa tarefa = Tarefa.nova("Escrever testes", RELOGIO);
            Clock depois = Clock.offset(RELOGIO, java.time.Duration.ofHours(2));

            tarefa.concluir(depois);

            assertThat(tarefa.isConcluida()).isTrue();
            assertThat(tarefa.getConcluidaEm()).isEqualTo(AGORA.plusSeconds(7200));
        }

        @Test
        void deveRecusarConcluirDuasVezes() {
            Tarefa tarefa = Tarefa.restaurar(UUID.randomUUID(), "Já feita", AGORA, AGORA);

            assertThatThrownBy(() -> tarefa.concluir(RELOGIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .extracting("codigo")
                    .isEqualTo("TAREFA_JA_CONCLUIDA");
        }
    }
}
