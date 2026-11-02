package io.github.rodrigomuller0018.sobramar.lancamento.dominio;

import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** O que aconteceu de verdade (ADR 0002, decisão 1). Transferência nunca conta como gasto ou receita. */
public record Lancamento(
        UUID id,
        UUID contaId,
        Tipo tipo,
        String descricao,
        long valorCentavos,
        LocalDate data,
        UUID carteiraId,
        UUID carteiraDestinoId,
        UUID compromissoId,
        Integer numeroOcorrencia,
        UUID pessoaId,
        UUID categoriaId,
        boolean ignorado,
        Instant criadoEm) {

    public enum Tipo {
        GASTO,
        RECEITA,
        TRANSFERENCIA
    }

    public static final int DESCRICAO_MAXIMA = 120;

    public Lancamento {
        if (valorCentavos <= 0) {
            throw new RegraDeNegocioException("VALOR_INVALIDO", "O valor precisa ser maior que zero.");
        }
        if (descricao == null || descricao.isBlank() || descricao.strip().length() > DESCRICAO_MAXIMA) {
            throw new RegraDeNegocioException("DESCRICAO_INVALIDA", "A descrição precisa ter de 1 a 120 caracteres.");
        }
        boolean transferencia = tipo == Tipo.TRANSFERENCIA;
        if (transferencia != (carteiraDestinoId != null) || carteiraId.equals(carteiraDestinoId)) {
            throw new RegraDeNegocioException(
                    "TRANSFERENCIA_INVALIDA", "A transferência exige um destino diferente da origem.");
        }
    }
}
