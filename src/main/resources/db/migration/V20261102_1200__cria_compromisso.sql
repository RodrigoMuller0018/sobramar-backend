-- O plano. As ocorrências são calculadas e nunca gravadas (ADR 0002).
CREATE TABLE compromisso (
    id                    uuid          NOT NULL,
    conta_id              uuid          NOT NULL,
    natureza              text          NOT NULL,
    forma                 text          NOT NULL,
    certeza               text          NOT NULL,
    descricao             text          NOT NULL,
    valor                 numeric(14,2) NOT NULL,
    parcelas              integer,
    data_inicio           date          NOT NULL,
    data_fim              date,
    recorrencia_anual     boolean       NOT NULL DEFAULT false,
    carteira_id           uuid,
    cartao_id             uuid,
    fatura_inicial        text,
    pessoa_id             uuid,
    categoria_id          uuid,
    cancelado_a_partir_de date,
    criado_em             timestamptz   NOT NULL,
    CONSTRAINT pk_compromisso PRIMARY KEY (id),
    CONSTRAINT fk_compromisso_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    CONSTRAINT fk_compromisso_carteira FOREIGN KEY (carteira_id) REFERENCES carteira (id),
    CONSTRAINT fk_compromisso_cartao FOREIGN KEY (cartao_id) REFERENCES cartao (id),
    CONSTRAINT fk_compromisso_pessoa FOREIGN KEY (pessoa_id) REFERENCES pessoa (id),
    CONSTRAINT fk_compromisso_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT ck_compromisso_natureza CHECK (natureza IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_compromisso_forma CHECK (forma IN ('PARCELADO', 'RECORRENTE', 'EVENTO')),
    CONSTRAINT ck_compromisso_certeza CHECK (certeza IN ('CERTO', 'ESTIMADO')),
    CONSTRAINT ck_compromisso_valor_positivo CHECK (valor > 0),
    CONSTRAINT ck_compromisso_origem_unica CHECK ((carteira_id IS NULL) <> (cartao_id IS NULL)),
    CONSTRAINT ck_compromisso_parcelas CHECK (
        (forma = 'PARCELADO' AND parcelas BETWEEN 1 AND 120) OR (forma <> 'PARCELADO' AND parcelas IS NULL)
    ),
    CONSTRAINT ck_compromisso_cartao_parcelas CHECK (cartao_id IS NULL OR parcelas IS NULL OR parcelas <= 48),
    CONSTRAINT ck_compromisso_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT ck_compromisso_fatura_formato CHECK (fatura_inicial IS NULL OR fatura_inicial ~ '^[0-9]{4}-[0-9]{2}$')
);

CREATE INDEX idx_compromisso_conta ON compromisso (conta_id);
CREATE INDEX idx_compromisso_carteira ON compromisso (carteira_id);
CREATE INDEX idx_compromisso_cartao ON compromisso (cartao_id);
CREATE INDEX idx_compromisso_pessoa ON compromisso (pessoa_id);
CREATE INDEX idx_compromisso_categoria ON compromisso (categoria_id);

-- Correção de uma parcela, sem gravar as outras.
CREATE TABLE ocorrencia_ajuste (
    id                uuid          NOT NULL,
    compromisso_id    uuid          NOT NULL,
    numero_ocorrencia integer       NOT NULL,
    valor             numeric(14,2),
    vencimento        date,
    fatura            text,
    ignorada          boolean       NOT NULL DEFAULT false,
    propaga           boolean       NOT NULL DEFAULT false,
    CONSTRAINT pk_ocorrencia_ajuste PRIMARY KEY (id),
    CONSTRAINT fk_ocorrencia_ajuste_compromisso FOREIGN KEY (compromisso_id)
        REFERENCES compromisso (id) ON DELETE CASCADE,
    CONSTRAINT uk_ocorrencia_ajuste UNIQUE (compromisso_id, numero_ocorrencia),
    CONSTRAINT ck_ocorrencia_ajuste_numero CHECK (numero_ocorrencia >= 1),
    CONSTRAINT ck_ocorrencia_ajuste_valor CHECK (valor IS NULL OR valor > 0),
    CONSTRAINT ck_ocorrencia_ajuste_fatura_formato CHECK (fatura IS NULL OR fatura ~ '^[0-9]{4}-[0-9]{2}$')
);
