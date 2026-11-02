-- Feature de exemplo do template. Migrações aplicadas são imutáveis: para mudar, crie uma nova.
CREATE TABLE tarefa (
    id           uuid        NOT NULL,
    titulo       text        NOT NULL,
    criada_em    timestamptz NOT NULL,
    concluida_em timestamptz,
    CONSTRAINT pk_tarefa PRIMARY KEY (id),
    CONSTRAINT ck_tarefa_titulo_tamanho CHECK (char_length(titulo) BETWEEN 1 AND 200),
    CONSTRAINT ck_tarefa_conclusao_apos_criacao CHECK (concluida_em IS NULL OR concluida_em >= criada_em)
);

CREATE INDEX idx_tarefa_criada_em ON tarefa (criada_em);
