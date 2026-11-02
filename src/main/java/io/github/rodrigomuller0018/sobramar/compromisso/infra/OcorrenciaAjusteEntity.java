package io.github.rodrigomuller0018.sobramar.compromisso.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ocorrencia_ajuste")
class OcorrenciaAjusteEntity {

    @Id
    UUID id;

    @Column(name = "compromisso_id", nullable = false)
    UUID compromissoId;

    @Column(name = "numero_ocorrencia", nullable = false)
    int numero;

    @Column(precision = 14, scale = 2)
    BigDecimal valor;

    LocalDate vencimento;

    String fatura;

    @Column(nullable = false)
    boolean ignorada;

    @Column(nullable = false)
    boolean propaga;

    protected OcorrenciaAjusteEntity() {
        // exigido pelo JPA
    }
}
