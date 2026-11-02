package io.github.rodrigomuller0018.sobramar.lancamento.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pagamento_fatura")
class PagamentoFaturaEntity {

    @Id
    UUID id;

    @Column(name = "conta_id", nullable = false)
    UUID contaId;

    @Column(name = "cartao_id", nullable = false)
    UUID cartaoId;

    @Column(nullable = false)
    String competencia;

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal valor;

    @Column(nullable = false)
    LocalDate data;

    @Column(name = "carteira_id", nullable = false)
    UUID carteiraId;

    @Column(name = "criado_em", nullable = false)
    Instant criadoEm;

    protected PagamentoFaturaEntity() {
        // exigido pelo JPA
    }

    static PagamentoFaturaEntity novo() {
        return new PagamentoFaturaEntity();
    }
}
