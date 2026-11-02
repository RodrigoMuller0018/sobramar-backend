-- O que aconteceu de verdade.
CREATE TABLE lancamento (
    id                  uuid          NOT NULL,
    conta_id            uuid          NOT NULL,
    tipo                text          NOT NULL,
    descricao           text          NOT NULL,
    valor               numeric(14,2) NOT NULL,
    data                date          NOT NULL,
    carteira_id         uuid          NOT NULL,
    carteira_destino_id uuid,
    compromisso_id      uuid,
    numero_ocorrencia   integer,
    pessoa_id           uuid,
    categoria_id        uuid,
    ignorado            boolean       NOT NULL DEFAULT false,
    criado_em           timestamptz   NOT NULL,
    CONSTRAINT pk_lancamento PRIMARY KEY (id),
    CONSTRAINT fk_lancamento_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    CONSTRAINT fk_lancamento_carteira FOREIGN KEY (carteira_id) REFERENCES carteira (id),
    CONSTRAINT fk_lancamento_carteira_destino FOREIGN KEY (carteira_destino_id) REFERENCES carteira (id),
    CONSTRAINT fk_lancamento_compromisso FOREIGN KEY (compromisso_id) REFERENCES compromisso (id),
    CONSTRAINT fk_lancamento_pessoa FOREIGN KEY (pessoa_id) REFERENCES pessoa (id),
    CONSTRAINT fk_lancamento_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT uk_lancamento_ocorrencia UNIQUE (compromisso_id, numero_ocorrencia),
    CONSTRAINT ck_lancamento_tipo CHECK (tipo IN ('GASTO', 'RECEITA', 'TRANSFERENCIA')),
    CONSTRAINT ck_lancamento_valor_positivo CHECK (valor > 0),
    CONSTRAINT ck_lancamento_transferencia CHECK (
        (tipo = 'TRANSFERENCIA' AND carteira_destino_id IS NOT NULL AND carteira_destino_id <> carteira_id)
        OR (tipo <> 'TRANSFERENCIA' AND carteira_destino_id IS NULL)
    ),
    CONSTRAINT ck_lancamento_ocorrencia_completa CHECK ((compromisso_id IS NULL) = (numero_ocorrencia IS NULL))
);

CREATE INDEX idx_lancamento_conta_data ON lancamento (conta_id, data);
CREATE INDEX idx_lancamento_carteira ON lancamento (carteira_id);
CREATE INDEX idx_lancamento_carteira_destino ON lancamento (carteira_destino_id);
CREATE INDEX idx_lancamento_pessoa ON lancamento (pessoa_id);
CREATE INDEX idx_lancamento_categoria ON lancamento (categoria_id);

-- Fora da sobra: as compras já foram contadas nas ocorrências.
CREATE TABLE pagamento_fatura (
    id          uuid          NOT NULL,
    conta_id    uuid          NOT NULL,
    cartao_id   uuid          NOT NULL,
    competencia text          NOT NULL,
    valor       numeric(14,2) NOT NULL,
    data        date          NOT NULL,
    carteira_id uuid          NOT NULL,
    criado_em   timestamptz   NOT NULL,
    CONSTRAINT pk_pagamento_fatura PRIMARY KEY (id),
    CONSTRAINT fk_pagamento_fatura_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    CONSTRAINT fk_pagamento_fatura_cartao FOREIGN KEY (cartao_id) REFERENCES cartao (id),
    CONSTRAINT fk_pagamento_fatura_carteira FOREIGN KEY (carteira_id) REFERENCES carteira (id),
    CONSTRAINT uk_pagamento_fatura UNIQUE (cartao_id, competencia),
    CONSTRAINT ck_pagamento_fatura_valor CHECK (valor > 0),
    CONSTRAINT ck_pagamento_fatura_competencia CHECK (competencia ~ '^[0-9]{4}-[0-9]{2}$')
);

CREATE INDEX idx_pagamento_fatura_conta ON pagamento_fatura (conta_id);
CREATE INDEX idx_pagamento_fatura_carteira ON pagamento_fatura (carteira_id);
