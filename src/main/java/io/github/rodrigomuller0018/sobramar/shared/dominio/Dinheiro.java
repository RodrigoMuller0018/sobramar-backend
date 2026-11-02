package io.github.rodrigomuller0018.sobramar.shared.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Conversão entre os centavos do domínio e da API e o numeric(14,2) do banco, sem double (ADR 0002). */
public final class Dinheiro {

    private static final int CASAS = 2;

    private Dinheiro() {}

    public static BigDecimal paraBanco(long centavos) {
        return BigDecimal.valueOf(centavos, CASAS);
    }

    public static long deBanco(BigDecimal valor) {
        return valor.setScale(CASAS, RoundingMode.UNNECESSARY)
                .movePointRight(CASAS)
                .longValueExact();
    }
}
