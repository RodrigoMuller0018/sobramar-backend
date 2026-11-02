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
@Table(name = "lancamento")
class LancamentoEntity {

    @Id
    UUID id;

    @Column(name = "conta_id", nullable = false)
    UUID contaId;

    @Column(nullable = false)
    String tipo;

    @Column(nullable = false)
    String descricao;

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal valor;

    @Column(nullable = false)
    LocalDate data;

    @Column(name = "carteira_id", nullable = false)
    UUID carteiraId;

    @Column(name = "carteira_destino_id")
    UUID carteiraDestinoId;

    @Column(name = "compromisso_id")
    UUID compromissoId;

    @Column(name = "numero_ocorrencia")
    Integer numeroOcorrencia;

    @Column(name = "pessoa_id")
    UUID pessoaId;

    @Column(name = "categoria_id")
    UUID categoriaId;

    @Column(nullable = false)
    boolean ignorado;

    @Column(name = "criado_em", nullable = false)
    Instant criadoEm;

    protected LancamentoEntity() {
        // exigido pelo JPA
    }
}
