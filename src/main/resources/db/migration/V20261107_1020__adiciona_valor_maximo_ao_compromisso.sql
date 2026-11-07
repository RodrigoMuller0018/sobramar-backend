-- Faixa de uma estimativa: 13º, férias e renda variável entram com o mínimo e mostram "pode chegar a".
-- Só vale em compromisso ESTIMADO e nunca é menor que o valor. Expand sem contract.
ALTER TABLE compromisso ADD COLUMN valor_maximo numeric(14,2);

ALTER TABLE compromisso ADD CONSTRAINT ck_compromisso_valor_maximo
    CHECK (valor_maximo IS NULL OR (certeza = 'ESTIMADO' AND valor_maximo >= valor));
