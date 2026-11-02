CREATE TABLE pessoa (
    id        uuid        NOT NULL,
    conta_id  uuid        NOT NULL,
    apelido   text        NOT NULL,
    arquivada boolean     NOT NULL DEFAULT false,
    criado_em timestamptz NOT NULL,
    CONSTRAINT pk_pessoa PRIMARY KEY (id),
    CONSTRAINT fk_pessoa_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    CONSTRAINT ck_pessoa_apelido_tamanho CHECK (char_length(apelido) BETWEEN 1 AND 40)
);

-- Apelido único por conta, sem diferenciar maiúsculas. Também serve de índice para a FK.
CREATE UNIQUE INDEX uk_pessoa_conta_apelido ON pessoa (conta_id, lower(apelido));
