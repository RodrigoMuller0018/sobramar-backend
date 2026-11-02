CREATE TABLE categoria (
    id        uuid        NOT NULL,
    conta_id  uuid        NOT NULL,
    nome      text        NOT NULL,
    arquivada boolean     NOT NULL DEFAULT false,
    criado_em timestamptz NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (id),
    CONSTRAINT fk_categoria_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    CONSTRAINT ck_categoria_nome_tamanho CHECK (char_length(nome) BETWEEN 1 AND 40)
);

-- Nome único por conta, sem diferenciar maiúsculas. Também serve de índice para a FK.
CREATE UNIQUE INDEX uk_categoria_conta_nome ON categoria (conta_id, lower(nome));
