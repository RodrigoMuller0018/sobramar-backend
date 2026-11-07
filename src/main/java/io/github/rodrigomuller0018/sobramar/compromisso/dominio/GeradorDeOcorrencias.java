package io.github.rodrigomuller0018.sobramar.compromisso.dominio;

import io.github.rodrigomuller0018.sobramar.carteira.dominio.RegraDeFatura;
import io.github.rodrigomuller0018.sobramar.compromisso.dominio.Compromisso.Forma;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * O coração da conta: calcula as ocorrências de um compromisso (ADR 0002, decisões 2, 3 e 6).
 * Função pura, sem relógio nem banco, para a simulação e os testes usarem exatamente a mesma regra.
 */
public final class GeradorDeOcorrencias {

    private GeradorDeOcorrencias() {}

    private record Base(LocalDate dataCompra, LocalDate vencimento, YearMonth competencia) {}

    /**
     * @param regra obrigatória em compromisso no cartão
     * @param ate até onde calcular o que não tem fim. Parcelado sempre sai completo.
     */
    public static List<Ocorrencia> gerar(
            Compromisso compromisso, RegraDeFatura regra, List<AjusteOcorrencia> ajustes, YearMonth ate) {
        Map<Integer, AjusteOcorrencia> porNumero =
                ajustes.stream().collect(Collectors.toMap(AjusteOcorrencia::numero, Function.identity()));
        List<AjusteOcorrencia> propagados = ajustes.stream()
                .filter(AjusteOcorrencia::propaga)
                .sorted(Comparator.comparingInt(AjusteOcorrencia::numero))
                .toList();

        List<Ocorrencia> ocorrencias = new ArrayList<>();
        for (int numero = 1; !acabou(compromisso, regra, numero, ate); numero++) {
            Ocorrencia ocorrencia = aplicarAjustes(compromisso, regra, numero, porNumero.get(numero), propagados);
            boolean cancelada = compromisso.canceladoAPartirDe() != null
                    && !ocorrencia.vencimento().isBefore(compromisso.canceladoAPartirDe());
            if (!cancelada) {
                ocorrencias.add(ocorrencia);
            }
        }
        return ocorrencias;
    }

    private static boolean acabou(Compromisso c, RegraDeFatura regra, int numero, YearMonth ate) {
        if (c.forma() == Forma.PARCELADO) {
            return numero > c.parcelas();
        }
        if (c.forma() == Forma.EVENTO && !c.recorrenciaAnual() && numero > 1) {
            return true;
        }
        Base base = base(c, regra, numero);
        boolean passouDoFim = c.dataFim() != null && base.dataCompra().isAfter(c.dataFim());
        return passouDoFim || base.competencia().isAfter(ate);
    }

    private static Ocorrencia aplicarAjustes(
            Compromisso c,
            RegraDeFatura regra,
            int numero,
            AjusteOcorrencia ajuste,
            List<AjusteOcorrencia> propagados) {
        Base base = base(c, regra, numero);
        long valor = valorBase(c, numero);
        for (AjusteOcorrencia propagado : propagados) {
            if (propagado.numero() <= numero && propagado.valorCentavos() != null) {
                valor = propagado.valorCentavos();
            }
        }
        if (ajuste == null) {
            return new Ocorrencia(numero, valor, base.dataCompra(), base.vencimento(), base.competencia(), false);
        }
        LocalDate vencimento = base.vencimento();
        YearMonth competencia = base.competencia();
        if (ajuste.fatura() != null) {
            RegraDeFatura.Fatura fatura = regra.faturaDaCompetencia(ajuste.fatura());
            vencimento = fatura.vencimento();
            competencia = fatura.competencia();
        }
        if (ajuste.vencimento() != null) {
            vencimento = ajuste.vencimento();
            competencia = YearMonth.from(vencimento);
        }
        return new Ocorrencia(
                numero,
                ajuste.valorCentavos() == null ? valor : ajuste.valorCentavos(),
                base.dataCompra(),
                vencimento,
                competencia,
                ajuste.ignorada());
    }

    /**
     * Parcela truncada em centavos, com a diferença na última: a soma é sempre igual ao total. A parte da conta
     * entra antes de dividir, para a soma das parcelas bater com a parte do total.
     */
    private static long valorBase(Compromisso c, int numero) {
        long total = c.valorDaParteCentavos();
        if (c.forma() != Forma.PARCELADO) {
            return total;
        }
        long parcela = total / c.parcelas();
        return numero == c.parcelas() ? total - parcela * (c.parcelas() - 1) : parcela;
    }

    private static Base base(Compromisso c, RegraDeFatura regra, int numero) {
        LocalDate data = dataDaOcorrencia(c, numero);
        if (!c.noCartao()) {
            LocalDate compra = c.forma() == Forma.PARCELADO ? c.dataInicio() : data;
            return new Base(compra, data, YearMonth.from(data));
        }
        RegraDeFatura.Fatura fatura = faturaDaOcorrencia(c, regra, numero, data);
        LocalDate compra = c.forma() == Forma.PARCELADO ? c.dataInicio() : data;
        return new Base(compra, fatura.vencimento(), fatura.competencia());
    }

    private static RegraDeFatura.Fatura faturaDaOcorrencia(
            Compromisso c, RegraDeFatura regra, int numero, LocalDate data) {
        if (c.faturaInicial() != null) {
            return regra.faturaDaParcela(c.faturaInicial(), numero - 1);
        }
        if (c.forma() == Forma.PARCELADO) {
            YearMonth primeira = regra.faturaDaCompra(c.dataInicio()).competencia();
            return regra.faturaDaParcela(primeira, numero - 1);
        }
        return regra.faturaDaCompra(data);
    }

    private static LocalDate dataDaOcorrencia(Compromisso c, int numero) {
        if (c.recorrenciaAnual()) {
            return c.dataInicio().plusYears(numero - 1L);
        }
        YearMonth mes = YearMonth.from(c.dataInicio()).plusMonths(numero - 1L);
        return RegraDeFatura.diaNoMes(mes, c.dataInicio().getDayOfMonth());
    }
}
