package io.github.rodrigomuller0018.sobramar.shared.dominio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** O vencimento é uma data civil no Brasil, não um instante em UTC. */
public final class Calendario {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Calendario() {}

    public static LocalDate hoje(Clock clock) {
        return LocalDate.ofInstant(clock.instant(), FUSO);
    }
}
