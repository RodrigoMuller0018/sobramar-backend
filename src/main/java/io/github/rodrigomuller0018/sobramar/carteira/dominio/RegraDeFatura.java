package io.github.rodrigomuller0018.sobramar.carteira.dominio;

import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Em qual fatura uma compra cai (ADR 0002, decisão 6). O resultado é uma sugestão: a regra do dia do
 * fechamento pode variar entre bancos, e a pessoa pode escolher outra fatura.
 */
public record RegraDeFatura(int diaFechamento, int diaVencimento) {

    private static final int DIA_MAXIMO = 31;

    /** A competência é o mês do vencimento, porque a sobra segue o regime de caixa. */
    public record Fatura(YearMonth competencia, LocalDate fechamento, LocalDate vencimento) {}

    public RegraDeFatura {
        if (diaFechamento < 1 || diaFechamento > DIA_MAXIMO || diaVencimento < 1 || diaVencimento > DIA_MAXIMO) {
            throw new RegraDeNegocioException("DIA_INVALIDO", "Os dias do cartão precisam estar entre 1 e 31.");
        }
    }

    public Fatura faturaDaCompra(LocalDate compra) {
        YearMonth mesDaCompra = YearMonth.from(compra);
        boolean antesDoFechamento = compra.isBefore(diaNoMes(mesDaCompra, diaFechamento));
        return faturaQueFechaEm(antesDoFechamento ? mesDaCompra : mesDaCompra.plusMonths(1));
    }

    /** A fatura que vence na competência informada. */
    public Fatura faturaDaCompetencia(YearMonth competencia) {
        return faturaQueFechaEm(venceNoMesDoFechamento() ? competencia : competencia.minusMonths(1));
    }

    /** A parcela {@code indice} (começando em 0) entra tantas faturas depois da primeira. */
    public Fatura faturaDaParcela(YearMonth primeiraCompetencia, int indice) {
        return faturaDaCompetencia(primeiraCompetencia.plusMonths(indice));
    }

    private Fatura faturaQueFechaEm(YearMonth mesDoFechamento) {
        YearMonth mesDoVencimento = venceNoMesDoFechamento() ? mesDoFechamento : mesDoFechamento.plusMonths(1);
        return new Fatura(
                mesDoVencimento, diaNoMes(mesDoFechamento, diaFechamento), diaNoMes(mesDoVencimento, diaVencimento));
    }

    private boolean venceNoMesDoFechamento() {
        return diaVencimento > diaFechamento;
    }

    /** Dia 31 em mês mais curto vira o último dia, sem acumular o ajuste. */
    public static LocalDate diaNoMes(YearMonth mes, int dia) {
        return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
    }
}
