package io.github.rodrigomuller0018.template.tarefa.dominio;

import io.github.rodrigomuller0018.template.shared.erro.RegraDeNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Regra de negócio pura: sem anotações de Spring ou JPA, testável em milissegundos. */
public final class Tarefa {

    public static final int TAMANHO_MAXIMO_TITULO = 200;

    private final UUID id;
    private final String titulo;
    private final Instant criadaEm;
    private Instant concluidaEm;

    private Tarefa(UUID id, String titulo, Instant criadaEm, Instant concluidaEm) {
        this.id = Objects.requireNonNull(id);
        this.titulo = validarTitulo(titulo);
        this.criadaEm = Objects.requireNonNull(criadaEm);
        this.concluidaEm = concluidaEm;
    }

    public static Tarefa nova(String titulo, Clock clock) {
        return new Tarefa(UUID.randomUUID(), titulo, clock.instant(), null);
    }

    /** Reconstrói uma tarefa já persistida. */
    public static Tarefa restaurar(UUID id, String titulo, Instant criadaEm, Instant concluidaEm) {
        return new Tarefa(id, titulo, criadaEm, concluidaEm);
    }

    public void concluir(Clock clock) {
        if (isConcluida()) {
            throw new RegraDeNegocioException("TAREFA_JA_CONCLUIDA", "A tarefa já foi concluída.");
        }
        this.concluidaEm = clock.instant();
    }

    public boolean isConcluida() {
        return concluidaEm != null;
    }

    private static String validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new RegraDeNegocioException("TITULO_OBRIGATORIO", "O título é obrigatório.");
        }
        String limpo = titulo.strip();
        if (limpo.length() > TAMANHO_MAXIMO_TITULO) {
            throw new RegraDeNegocioException(
                    "TITULO_MUITO_LONGO", "O título deve ter no máximo " + TAMANHO_MAXIMO_TITULO + " caracteres.");
        }
        return limpo;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getConcluidaEm() {
        return concluidaEm;
    }
}
