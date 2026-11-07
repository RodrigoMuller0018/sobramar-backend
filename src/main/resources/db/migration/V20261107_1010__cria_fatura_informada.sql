-- O total que o banco cobrou numa fatura, informado pela pessoa antes de pagar ("bate com minha fatura?").
-- Serve só para conciliar a fatura calculada; não é pagamento nem gasto.
CREATE TABLE fatura_informada (
    id           uuid          NOT NULL,
    conta_id     uuid          NOT NULL,
    cartao_id    uuid          NOT NULL,
    competencia  text          NOT NULL,
    total        numeric(14,2) NOT NULL,
    informado_em timestamptz   NOT NULL,
    CONSTRAINT pk_fatura_informada PRIMARY KEY (id),
    CONSTRAINT fk_fatura_informada_conta FOREIGN KEY (conta_id) REFERENCES conta (id) ON DELETE CASCADE,
    CONSTRAINT fk_fatura_informada_cartao FOREIGN KEY (cartao_id) REFERENCES cartao (id),
    CONSTRAINT uk_fatura_informada UNIQUE (cartao_id, competencia),
    CONSTRAINT ck_fatura_informada_total CHECK (total > 0),
    CONSTRAINT ck_fatura_informada_competencia CHECK (competencia ~ '^[0-9]{4}-[0-9]{2}$')
);

CREATE INDEX idx_fatura_informada_conta ON fatura_informada (conta_id);
