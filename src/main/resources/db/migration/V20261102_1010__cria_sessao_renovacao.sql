-- Guarda só o hash do refresh token: um vazamento desta tabela não permite abrir sessões.
CREATE TABLE sessao_renovacao (
    id          uuid        NOT NULL,
    usuario_id  uuid        NOT NULL,
    token_hash  text        NOT NULL,
    expira_em   timestamptz NOT NULL,
    revogada_em timestamptz,
    criado_em   timestamptz NOT NULL,
    CONSTRAINT pk_sessao_renovacao PRIMARY KEY (id),
    CONSTRAINT fk_sessao_renovacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT uk_sessao_renovacao_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_sessao_renovacao_usuario ON sessao_renovacao (usuario_id);
