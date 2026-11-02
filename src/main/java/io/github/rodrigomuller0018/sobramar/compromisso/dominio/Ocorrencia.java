package io.github.rodrigomuller0018.sobramar.compromisso.dominio;

import java.time.LocalDate;
import java.time.YearMonth;

/** Um vencimento calculado de um compromisso. Não é tabela. A competência é o mês em que o dinheiro se move. */
public record Ocorrencia(
        int numero,
        long valorCentavos,
        LocalDate dataCompra,
        LocalDate vencimento,
        YearMonth competencia,
        boolean ignorada) {}
