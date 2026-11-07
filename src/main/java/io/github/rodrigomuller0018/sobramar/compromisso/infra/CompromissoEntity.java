package io.github.rodrigomuller0018.sobramar.compromisso.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "compromisso")
class CompromissoEntity {

    @Id
    UUID id;

    @Column(name = "conta_id", nullable = false)
    UUID contaId;

    @Column(nullable = false)
    String natureza;

    @Column(nullable = false)
    String forma;

    @Column(nullable = false)
    String certeza;

    @Column(nullable = false)
    String descricao;

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal valor;

    Integer parcelas;

    @Column(name = "data_inicio", nullable = false)
    LocalDate dataInicio;

    @Column(name = "data_fim")
    LocalDate dataFim;

    @Column(name = "recorrencia_anual", nullable = false)
    boolean recorrenciaAnual;

    @Column(name = "carteira_id")
    UUID carteiraId;

    @Column(name = "cartao_id")
    UUID cartaoId;

    @Column(name = "fatura_inicial")
    String faturaInicial;

    @Column(name = "pessoa_id")
    UUID pessoaId;

    @Column(name = "categoria_id")
    UUID categoriaId;

    Integer parte;

    @Column(name = "valor_maximo", precision = 14, scale = 2)
    BigDecimal valorMaximo;

    @Column(name = "cancelado_a_partir_de")
    LocalDate canceladoAPartirDe;

    @Column(name = "criado_em", nullable = false)
    Instant criadoEm;

    protected CompromissoEntity() {
        // exigido pelo JPA
    }
}
