CREATE TABLE carteira (
    id            uuid          NOT NULL,
    conta_id      uuid          NOT NULL,
    tipo          text          NOT NULL,
    nome          text          NOT NULL,
    saldo_inicial numeric(14,2) NOT NULL,
    arquivada     boolean       NOT NULL DEFAULT false,
    criado_em     timestamptz   NOT NULL,
    CONSTRAINT pk_carteira PRIMARY KEY (id),
    CONSTRAINT fk_carteira_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    CONSTRAINT ck_carteira_tipo CHECK (tipo IN ('CONTA', 'DINHEIRO', 'VALE')),
    CONSTRAINT ck_carteira_nome_tamanho CHECK (char_length(nome) BETWEEN 1 AND 60)
);

CREATE INDEX idx_carteira_conta ON carteira (conta_id);

CREATE TABLE cartao (
    id             uuid          NOT NULL,
    conta_id       uuid          NOT NULL,
    nome           text          NOT NULL,
    dia_fechamento integer       NOT NULL,
    dia_vencimento integer       NOT NULL,
    limite         numeric(14,2),
    arquivado      boolean       NOT NULL DEFAULT false,
    criado_em      timestamptz   NOT NULL,
    CONSTRAINT pk_cartao PRIMARY KEY (id),
    CONSTRAINT fk_cartao_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    CONSTRAINT ck_cartao_nome_tamanho CHECK (char_length(nome) BETWEEN 1 AND 60),
    CONSTRAINT ck_cartao_dia_fechamento CHECK (dia_fechamento BETWEEN 1 AND 31),
    CONSTRAINT ck_cartao_dia_vencimento CHECK (dia_vencimento BETWEEN 1 AND 31),
    CONSTRAINT ck_cartao_limite_positivo CHECK (limite IS NULL OR limite >= 0)
);

CREATE INDEX idx_cartao_conta ON cartao (conta_id);
