package io.github.rodrigomuller0018.sobramar.carteira.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "carteira")
class CarteiraEntity {

    @Id
    private UUID id;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String nome;

    @Column(name = "saldo_inicial", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoInicial;

    @Column(nullable = false)
    private boolean arquivada;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected CarteiraEntity() {
        // exigido pelo JPA
    }

    CarteiraEntity(
            UUID id,
            UUID contaId,
            String tipo,
            String nome,
            BigDecimal saldoInicial,
            boolean arquivada,
            Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.tipo = tipo;
        this.nome = nome;
        this.saldoInicial = saldoInicial;
        this.arquivada = arquivada;
        this.criadoEm = criadoEm;
    }

    UUID getId() {
        return id;
    }

    UUID getContaId() {
        return contaId;
    }

    String getTipo() {
        return tipo;
    }

    String getNome() {
        return nome;
    }

    BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    boolean isArquivada() {
        return arquivada;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
