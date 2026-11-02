package io.github.rodrigomuller0018.sobramar.compromisso.dominio;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Correção de uma ocorrência, sem gravar as outras. Campos nulos mantêm o calculado.
 * Com {@code propaga}, o valor vale também para as ocorrências seguintes.
 */
public record AjusteOcorrencia(
        int numero, Long valorCentavos, LocalDate vencimento, YearMonth fatura, boolean ignorada, boolean propaga) {

    public static AjusteOcorrencia vazio(int numero) {
        return new AjusteOcorrencia(numero, null, null, null, false, false);
    }

    public AjusteOcorrencia semValor() {
        return new AjusteOcorrencia(numero, null, vencimento, fatura, ignorada, false);
    }
}
