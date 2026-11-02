package io.github.rodrigomuller0018.sobramar.carteira.dominio;

import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Instant;
import java.util.UUID;

/** Cartão de crédito. A fatura é calculada por {@link RegraDeFatura}, não é uma entidade. */
public class Cartao {

    public static final int NOME_MAXIMO = 60;

    private final UUID id;
    private final UUID contaId;
    private final Instant criadoEm;
    private String nome;
    private RegraDeFatura regra;
    private Long limiteCentavos;
    private boolean arquivado;

    private Cartao(
            UUID id,
            UUID contaId,
            String nome,
            RegraDeFatura regra,
            Long limiteCentavos,
            boolean arquivado,
            Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.nome = nome;
        this.regra = regra;
        this.limiteCentavos = limiteCentavos;
        this.arquivado = arquivado;
        this.criadoEm = criadoEm;
    }

    public static Cartao novo(UUID contaId, String nome, RegraDeFatura regra, Long limiteCentavos, Instant agora) {
        return new Cartao(
                UUID.randomUUID(), contaId, validarNome(nome), regra, validarLimite(limiteCentavos), false, agora);
    }

    public static Cartao restaurar(
            UUID id,
            UUID contaId,
            String nome,
            RegraDeFatura regra,
            Long limiteCentavos,
            boolean arquivado,
            Instant criadoEm) {
        return new Cartao(id, contaId, nome, regra, limiteCentavos, arquivado, criadoEm);
    }

    public void atualizar(String novoNome, RegraDeFatura novaRegra, Long novoLimiteCentavos) {
        this.nome = validarNome(novoNome);
        this.regra = novaRegra;
        this.limiteCentavos = validarLimite(novoLimiteCentavos);
    }

    public void arquivar() {
        this.arquivado = true;
    }

    private static String validarNome(String nome) {
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isEmpty() || limpo.length() > NOME_MAXIMO) {
            throw new RegraDeNegocioException("NOME_INVALIDO", "O nome precisa ter de 1 a 60 caracteres.");
        }
        return limpo;
    }

    private static Long validarLimite(Long limite) {
        if (limite != null && limite < 0) {
            throw new RegraDeNegocioException("VALOR_INVALIDO", "O limite não pode ser negativo.");
        }
        return limite;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getNome() {
        return nome;
    }

    public RegraDeFatura getRegra() {
        return regra;
    }

    public Long getLimiteCentavos() {
        return limiteCentavos;
    }

    public boolean isArquivado() {
        return arquivado;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
