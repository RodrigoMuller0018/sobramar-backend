package io.github.rodrigomuller0018.sobramar.lancamento.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fatura_informada")
class FaturaInformadaEntity {

    @Id
    UUID id;

    @Column(name = "conta_id", nullable = false)
    UUID contaId;

    @Column(name = "cartao_id", nullable = false)
    UUID cartaoId;

    @Column(nullable = false)
    String competencia;

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal total;

    @Column(name = "informado_em", nullable = false)
    Instant informadoEm;

    protected FaturaInformadaEntity() {
        // exigido pelo JPA
    }

    static FaturaInformadaEntity novo() {
        return new FaturaInformadaEntity();
    }
}
