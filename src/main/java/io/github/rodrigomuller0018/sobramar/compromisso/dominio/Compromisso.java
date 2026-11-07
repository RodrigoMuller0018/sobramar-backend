package io.github.rodrigomuller0018.sobramar.compromisso.dominio;

import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * O plano: o que está combinado para o futuro (ADR 0002, decisão 1). O futuro é calculado a partir daqui,
 * nunca gravado. {@code valorCentavos} é o total em PARCELADO e o valor de cada ocorrência nas demais formas.
 */
public record Compromisso(
        UUID id,
        UUID contaId,
        Natureza natureza,
        Forma forma,
        Certeza certeza,
        String descricao,
        long valorCentavos,
        Integer parcelas,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean recorrenciaAnual,
        UUID carteiraId,
        UUID cartaoId,
        YearMonth faturaInicial,
        UUID pessoaId,
        UUID categoriaId,
        Integer parte,
        Long valorMaximoCentavos,
        LocalDate canceladoAPartirDe,
        Instant criadoEm) {

    public enum Natureza {
        ENTRADA,
        SAIDA
    }

    public enum Forma {
        PARCELADO,
        RECORRENTE,
        EVENTO
    }

    public enum Certeza {
        CERTO,
        ESTIMADO
    }

    public static final int DESCRICAO_MAXIMA = 120;
    public static final int MAXIMO_PARCELAS_CARTAO = 48;
    public static final int MAXIMO_PARCELAS_BOLETO = 120;
    public static final int PARTE_INTEIRA = 100;

    public Compromisso {
        validarDescricao(descricao);
        validarOrigem(natureza, carteiraId, cartaoId, faturaInicial);
        validarParcelas(forma, parcelas, cartaoId);
        if (parte != null && (parte < 1 || parte > PARTE_INTEIRA)) {
            throw new RegraDeNegocioException("PARTE_INVALIDA", "A parte precisa ficar entre 1 e 100 por cento.");
        }
        if (valorMaximoCentavos != null && (certeza != Certeza.ESTIMADO || valorMaximoCentavos < valorCentavos)) {
            throw new RegraDeNegocioException(
                    "VALOR_MAXIMO_INVALIDO", "O valor máximo só vale em estimativa e não pode ser menor que o valor.");
        }
        if (valorCentavos <= 0) {
            throw new RegraDeNegocioException("VALOR_INVALIDO", "O valor precisa ser maior que zero.");
        }
        if (dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new RegraDeNegocioException("PERIODO_INVALIDO", "A data final vem antes da inicial.");
        }
        if (recorrenciaAnual && forma != Forma.EVENTO) {
            throw new RegraDeNegocioException("FORMA_INVALIDA", "Só eventos podem repetir todo ano.");
        }
    }

    public boolean noCartao() {
        return cartaoId != null;
    }

    /**
     * A parte deste compromisso que é da conta, em centavos: quem divide o aluguel só vê a própria metade.
     * Meio centavo arredonda para cima. Sem parte, vale o valor inteiro.
     */
    public long valorDaParteCentavos() {
        if (parte == null || parte == PARTE_INTEIRA) {
            return valorCentavos;
        }
        return (valorCentavos * parte + PARTE_INTEIRA / 2) / PARTE_INTEIRA;
    }

    public Compromisso cancelado(LocalDate aPartirDe) {
        return new Compromisso(
                id,
                contaId,
                natureza,
                forma,
                certeza,
                descricao,
                valorCentavos,
                parcelas,
                dataInicio,
                dataFim,
                recorrenciaAnual,
                carteiraId,
                cartaoId,
                faturaInicial,
                pessoaId,
                categoriaId,
                parte,
                valorMaximoCentavos,
                aPartirDe,
                criadoEm);
    }

    private static void validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank() || descricao.strip().length() > DESCRICAO_MAXIMA) {
            throw new RegraDeNegocioException("DESCRICAO_INVALIDA", "A descrição precisa ter de 1 a 120 caracteres.");
        }
    }

    private static void validarOrigem(Natureza natureza, UUID carteiraId, UUID cartaoId, YearMonth faturaInicial) {
        if (carteiraId == null && cartaoId == null) {
            throw new RegraDeNegocioException("ORIGEM_OBRIGATORIA", "Informe a carteira ou o cartão.");
        }
        if (carteiraId != null && cartaoId != null) {
            throw new RegraDeNegocioException("ORIGEM_DUPLA", "Informe a carteira ou o cartão, não os dois.");
        }
        if (natureza == Natureza.ENTRADA && cartaoId != null) {
            throw new RegraDeNegocioException("CARTEIRA_INCOMPATIVEL", "Entrada não pode ir para um cartão.");
        }
        if (faturaInicial != null && cartaoId == null) {
            throw new RegraDeNegocioException("CARTEIRA_INCOMPATIVEL", "Só compra no cartão tem fatura.");
        }
    }

    private static void validarParcelas(Forma forma, Integer parcelas, UUID cartaoId) {
        if (forma != Forma.PARCELADO) {
            if (parcelas != null) {
                throw new RegraDeNegocioException("PARCELAS_INVALIDAS", "Só compromisso parcelado tem parcelas.");
            }
            return;
        }
        if (parcelas == null || parcelas < 1) {
            throw new RegraDeNegocioException("PARCELAS_INVALIDAS", "Informe ao menos uma parcela.");
        }
        if (cartaoId != null && parcelas > MAXIMO_PARCELAS_CARTAO) {
            throw new RegraDeNegocioException("CARTAO_LIMITE_PARCELAS", "No cartão, são até 48 parcelas.");
        }
        if (cartaoId == null && parcelas > MAXIMO_PARCELAS_BOLETO) {
            throw new RegraDeNegocioException("BOLETO_LIMITE_PARCELAS", "No boleto, são até 120 parcelas.");
        }
    }
}
