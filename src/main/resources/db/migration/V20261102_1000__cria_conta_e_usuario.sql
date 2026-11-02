CREATE TABLE conta (
    id        uuid        NOT NULL,
    nome      text        NOT NULL,
    criado_em timestamptz NOT NULL,
    CONSTRAINT pk_conta PRIMARY KEY (id),
    CONSTRAINT ck_conta_nome_tamanho CHECK (char_length(nome) BETWEEN 1 AND 80)
);

CREATE TABLE usuario (
    id                   uuid        NOT NULL,
    conta_id             uuid        NOT NULL,
    email                text        NOT NULL,
    senha_hash           text        NOT NULL,
    totp_segredo_cifrado text,
    totp_ativo           boolean     NOT NULL DEFAULT false,
    totp_ultimo_passo    bigint,
    criado_em            timestamptz NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT fk_usuario_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_email_minusculo CHECK (email = lower(email)),
    CONSTRAINT ck_usuario_totp_ativo_exige_segredo CHECK (NOT totp_ativo OR totp_segredo_cifrado IS NOT NULL)
);

CREATE INDEX idx_usuario_conta ON usuario (conta_id);
