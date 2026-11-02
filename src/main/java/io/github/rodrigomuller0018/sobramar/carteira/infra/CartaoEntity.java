package io.github.rodrigomuller0018.sobramar.carteira.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cartao")
class CartaoEntity {

    @Id
    private UUID id;

    @Column(name = "conta_id", nullable = false)
    private UUID contaId;

    @Column(nullable = false)
    private String nome;

    @Column(name = "dia_fechamento", nullable = false)
    private int diaFechamento;

    @Column(name = "dia_vencimento", nullable = false)
    private int diaVencimento;

    @Column(precision = 14, scale = 2)
    private BigDecimal limite;

    @Column(nullable = false)
    private boolean arquivado;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected CartaoEntity() {
        // exigido pelo JPA
    }

    CartaoEntity(
            UUID id,
            UUID contaId,
            String nome,
            int diaFechamento,
            int diaVencimento,
            BigDecimal limite,
            boolean arquivado,
            Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.nome = nome;
        this.diaFechamento = diaFechamento;
        this.diaVencimento = diaVencimento;
        this.limite = limite;
        this.arquivado = arquivado;
        this.criadoEm = criadoEm;
    }

    UUID getId() {
        return id;
    }

    UUID getContaId() {
        return contaId;
    }

    String getNome() {
        return nome;
    }

    int getDiaFechamento() {
        return diaFechamento;
    }

    int getDiaVencimento() {
        return diaVencimento;
    }

    BigDecimal getLimite() {
        return limite;
    }

    boolean isArquivado() {
        return arquivado;
    }

    Instant getCriadoEm() {
        return criadoEm;
    }
}
