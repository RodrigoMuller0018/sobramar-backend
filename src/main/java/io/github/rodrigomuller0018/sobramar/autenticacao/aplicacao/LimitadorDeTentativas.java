package io.github.rodrigomuller0018.sobramar.autenticacao.aplicacao;

import io.github.rodrigomuller0018.sobramar.shared.erro.MuitasTentativasException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Atrapalha força bruta no login. Fica em memória, o que basta para uma instância só;
 * a Entrega C reavalia isso antes de abrir ao público.
 */
@Component
public class LimitadorDeTentativas {

    static final int MAXIMO_DE_FALHAS = 5;
    static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private record Falhas(int quantidade, Instant primeira) {}

    private final Map<String, Falhas> falhas = new ConcurrentHashMap<>();
    private final Clock clock;

    public LimitadorDeTentativas(Clock clock) {
        this.clock = clock;
    }

    public void verificar(String chave) {
        Falhas atual = falhas.get(chave);
        if (atual == null) {
            return;
        }
        Instant agora = clock.instant();
        Instant fim = atual.primeira().plus(BLOQUEIO);
        if (!agora.isBefore(fim)) {
            falhas.remove(chave);
        } else if (atual.quantidade() >= MAXIMO_DE_FALHAS) {
            throw new MuitasTentativasException(Duration.between(agora, fim).toSeconds() + 1);
        }
    }

    public void registrarFalha(String chave) {
        Instant agora = clock.instant();
        falhas.merge(chave, new Falhas(1, agora), (a, n) -> new Falhas(a.quantidade() + 1, a.primeira()));
    }

    public void limpar(String chave) {
        falhas.remove(chave);
    }
}
