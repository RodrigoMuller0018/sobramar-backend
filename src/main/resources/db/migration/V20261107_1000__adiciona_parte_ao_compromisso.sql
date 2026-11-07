-- A parte do compromisso que é da conta, em por cento (quem divide o aluguel só vê a própria metade).
-- Nulo significa o valor inteiro. Expand sem contract: nada do que existe muda.
ALTER TABLE compromisso ADD COLUMN parte integer;

ALTER TABLE compromisso ADD CONSTRAINT ck_compromisso_parte CHECK (parte IS NULL OR parte BETWEEN 1 AND 100);
