package io.github.rodrigomuller0018.sobramar.carteira.dominio;

import io.github.rodrigomuller0018.sobramar.shared.erro.RegraDeNegocioException;
import java.time.Instant;
import java.util.UUID;

/** Conta bancária, dinheiro ou vale. O saldo é informado pela pessoa, nunca vem de banco. */
public class Carteira {

    public enum Tipo {
        CONTA,
        DINHEIRO,
        VALE
    }

    public static final int NOME_MAXIMO = 60;

    private final UUID id;
    private final UUID contaId;
    private final Tipo tipo;
    private final Instant criadoEm;
    private String nome;
    private long saldoInicialCentavos;
    private boolean arquivada;

    private Carteira(
            UUID id,
            UUID contaId,
            Tipo tipo,
            String nome,
            long saldoInicialCentavos,
            boolean arquivada,
            Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.tipo = tipo;
        this.nome = nome;
        this.saldoInicialCentavos = saldoInicialCentavos;
        this.arquivada = arquivada;
        this.criadoEm = criadoEm;
    }

    public static Carteira nova(UUID contaId, Tipo tipo, String nome, long saldoInicialCentavos, Instant agora) {
        return new Carteira(UUID.randomUUID(), contaId, tipo, validarNome(nome), saldoInicialCentavos, false, agora);
    }

    public static Carteira restaurar(
            UUID id,
            UUID contaId,
            Tipo tipo,
            String nome,
            long saldoInicialCentavos,
            boolean arquivada,
            Instant criadoEm) {
        return new Carteira(id, contaId, tipo, nome, saldoInicialCentavos, arquivada, criadoEm);
    }

    /** O tipo não muda: transformar um vale em conta bagunçaria a sobra de meses já calculados. */
    public void atualizar(Tipo novoTipo, String novoNome, long novoSaldoInicialCentavos) {
        if (novoTipo != tipo) {
            throw new RegraDeNegocioException("CARTEIRA_TIPO_IMUTAVEL", "O tipo da carteira não pode mudar.");
        }
        this.nome = validarNome(novoNome);
        this.saldoInicialCentavos = novoSaldoInicialCentavos;
    }

    public void arquivar() {
        this.arquivada = true;
    }

    private static String validarNome(String nome) {
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isEmpty() || limpo.length() > NOME_MAXIMO) {
            throw new RegraDeNegocioException("NOME_INVALIDO", "O nome precisa ter de 1 a 60 caracteres.");
        }
        return limpo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getNome() {
        return nome;
    }

    public long getSaldoInicialCentavos() {
        return saldoInicialCentavos;
    }

    public boolean isArquivada() {
        return arquivada;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
