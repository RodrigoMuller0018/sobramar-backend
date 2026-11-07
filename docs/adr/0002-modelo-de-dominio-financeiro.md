# ADR 0002: Modelo de domínio financeiro

- **Status:** proposto
- **Data:** 2026-11-02
- **Contrato relacionado:** [docs/api/CONTRATO.md](../api/CONTRATO.md)

## Contexto

O Sobramar responde "quanto vai sobrar em cada um dos próximos meses?". Essa resposta precisa ser confiável, porque saldo errado é a principal reclamação encontrada na pesquisa de mercado. O modelo também precisa aceitar, nas próximas entregas, consórcio, financiamento e investimentos sem reescrever o núcleo.

Este ADR cobre a Entrega A. Tudo é desenho: nada foi implementado ou testado ainda.

## Decisões

### 1. Compromisso é o plano; lançamento é o que aconteceu

Toda a modelagem parte desta separação:

- **`Compromisso`:** o que está combinado para o futuro, como uma compra em 12x, o aluguel, o salário ou o IPVA.
- **`Lancamento`:** o que aconteceu de verdade, como um gasto avulso ou o pagamento de uma parcela.
- **`Ocorrencia`:** cada vencimento calculado de um compromisso. **Não é uma tabela.**

Um compromisso responde a três perguntas, que permitem acrescentar casos novos sem criar um modelo para cada um:

| Pergunta | Campo | Valores na Entrega A | Depois |
|---|---|---|---|
| Quando acontece? | `forma` | `PARCELADO`, `RECORRENTE`, `EVENTO` | `CONSORCIO`, `FINANCIAMENTO` (Entrega B) |
| Quão certo é? | `certeza` | `CERTO`, `ESTIMADO` | `INCERTO` (Entrega B) |
| De onde sai ou para onde vai? | `carteiraId` ou `cartaoId` | Carteira ou cartão | |

Além disso, `natureza` indica `ENTRADA` ou `SAIDA`.

### 2. O futuro é calculado, nunca gravado

As ocorrências são geradas na hora a partir do compromisso, por uma função pura do domínio: `Projecao = f(compromissos, lancamentos, cartões, período)`.

- **Vantagem:** alterar um contrato atualiza os meses seguintes sem migrar dados, e a simulação usa exatamente a mesma conta.
- **Como o realizado entra:** um `Lancamento` aponta para `compromisso_id` + `numero_ocorrencia` e substitui o previsto daquela ocorrência.
- **Custo:** calcular a cada leitura. O período é limitado a 24 meses e o índice por `conta_id` mantém a consulta pequena. Se isso pesar, a medição decide se vale um cache.

**Alternativa rejeitada:** gravar uma linha por parcela. É mais simples de consultar, mas cada edição exigiria reescrever linhas e abriria espaço para divergência, justamente o defeito que este produto quer evitar.

### 3. Dinheiro

- **Domínio:** um value object `Dinheiro`, em centavos, sem `double`.
- **API:** inteiro em centavos (`valorCentavos`). O JavaScript representa inteiros desse tamanho com exatidão, e a formatação fica no front.
- **Banco:** `numeric(14,2)`, como manda o manual. O adaptador converte.
- **Moeda:** só BRL. Multimoeda é opcional e não entra agora.
- **Arredondamento:** a parcela é `total / n` truncada em centavos, e a diferença vai na última. R$ 100,00 em 3x = 33,33 + 33,33 + 33,34. A soma das parcelas é sempre igual ao total.

### 4. Datas e competência

- **Eventos financeiros:** `LocalDate` (`date` no banco), sem fuso. Um vencimento em 10/03 é 10/03 em qualquer lugar.
- **Auditoria:** `Instant` (`timestamptz`), como `criadoEm`.
- **Mês:** `YearMonth`, no formato `2027-03` na API.
- **Fim de mês:** dia 31 em mês mais curto vira o último dia daquele mês, sem acumular o ajuste. Uma recorrência no dia 31 cai em 28/02 e volta a 31/03.
- **Relógio:** sempre o `Clock` injetado. "Hoje" nunca vem de `now()` solto.

### 5. A sobra segue o regime de caixa

O mês de um valor é o mês em que o dinheiro sai ou entra:

- Compra no cartão conta no mês de **vencimento da fatura**, não no mês da compra.
- Boleto conta no mês do vencimento.
- Receita conta no mês do recebimento.

```
sobra(mês) = receitas(mês) − compromissos(mês)
```

Sem carteira de vale, sem pagamento de fatura e sem transferência. A sobra de um mês **não** passa para o seguinte na Entrega A, porque a pergunta é "quanto sobra neste mês". Um saldo acumulado pode ser acrescentado sem mudar o restante.

### 6. Regras de cartão e fatura

A fatura é calculada, não é uma entidade própria. Com `F` = dia de fechamento e `V` = dia de vencimento:

1. Compra **antes** do dia `F`: entra na fatura que fecha naquele mês.
2. Compra **no dia `F` ou depois**: entra na fatura do mês seguinte.
3. Se `V > F`, a fatura vence no mesmo mês em que fecha. Se `V <= F`, no mês seguinte.
4. A parcela `i` (começando em 0) entra `i` faturas depois da primeira.

**Ainda não confirmado:** a regra 2 pode variar entre bancos, porque alguns tratam o dia do fechamento de outra forma.

Por isso, o cálculo é uma **sugestão**, e a pessoa pode escolher outra fatura (`fatura_inicial`). Quando a escolha não combinar com a data da compra, como colocar uma compra de novembro numa fatura fechada em outubro, a API devolve um aviso (`FATURA_INCOMPATIVEL_COM_DATA`), sem bloquear. A correção é aceita porque a pessoa conhece a fatura real dela. O aviso evita que um engano passe despercebido.

**Data da compra e vencimento são coisas diferentes** e aparecem separadas: a data da compra não muda entre as parcelas, e o vencimento é o da fatura de cada uma.

**Limite do cartão:** o limite usado soma todas as parcelas ainda não pagas, não só as da fatura atual, porque é o que os bancos costumam comprometer. Isso é calculado, e a confirmação depende do comportamento de cada banco.

### 6.1. Editar e ignorar

Editar uma parcela pede um escopo:

| Escopo | Efeito |
|---|---|
| `SOMENTE_ESTA` | Grava um ajuste só naquela ocorrência |
| `ESTA_E_FUTURAS` | Ajusta o plano a partir daquela ocorrência |
| `TODAS` | Altera todo o plano. Ocorrências já pagas mantêm o valor real |

O ajuste de uma ocorrência fica em `ocorrencia_ajuste`, sem gravar as outras parcelas. Isso mantém a decisão 2: o futuro continua calculado.

Para não gravar parcelas nem na edição em massa, `ESTA_E_FUTURAS` e `TODAS` guardam só o valor, em um ajuste com `propaga`.

**Ignorar** tira um item dos totais sem apagá-lo. Vale para ocorrência e para lançamento. O item continua visível, marcado como ignorado.

### 7. O que nunca é gasto nem receita

Estas regras impedem a contagem dupla:

- **Pagamento de fatura:** registrado como `PagamentoFatura`, fora da sobra. As compras já foram contadas nas ocorrências.
- **Transferência entre carteiras:** um `Lancamento` do tipo `TRANSFERENCIA`, fora da sobra.
- **Vale:** carteira do tipo `VALE`. Crédito e gastos ficam num bloco próprio da projeção, e nunca entram na sobra. Uma receita de vale precisa apontar para uma carteira `VALE`, e o banco e o domínio impedem misturar.

### 8. Conta, usuário e pessoa

- **`Usuario`:** quem faz login.
- **`Conta`:** dona dos dados. Na Entrega A é sempre um usuário para uma conta. Separar agora evita migrar todas as tabelas se um dia houver convites.
- **`Pessoa`:** marcador com apelido, sem CPF, nascimento ou contato. Uma pessoa da família pode registrar tudo sozinha. `pessoa_id` é opcional.

O filtro por pessoa mostra só os itens dela, e o total da conta soma todos. Não existe visão que junte contas diferentes.

### 9. Isolamento entre contas

- Toda tabela de dados tem `conta_id NOT NULL` com FK, e toda consulta filtra por ele.
- O `contaId` vem **sempre** da sessão, nunca da URL ou do corpo.
- Recurso de outra conta responde **404**, igual a um recurso inexistente, para não revelar que ele existe.
- Referências internas também são validadas: um compromisso não pode apontar para o cartão de outra conta.
- Há teste automático de acesso cruzado em todos os endpoints.
- Row-level security do PostgreSQL entra na Entrega C como segunda defesa.

### 10. Autenticação

- **Credenciais:** senha com hash Argon2id e segundo fator TOTP.
- **Sessão:** access token JWT curto e refresh token rotativo, ambos em cookie `HttpOnly`, `Secure` e `SameSite=Strict`, com proteção CSRF. Nada em `localStorage`.
- **Conta inicial:** criada por configuração no ambiente, sem tela pública de cadastro e sem segredo no Git.
- **Limites:** limite de tentativas no login e mensagem de erro genérica.
- **Ressalva:** implementar a emissão de JWT aqui é escolha de estudo, permitida pelo manual fora de produção. A Entrega C reavalia a troca por um provedor como o Keycloak antes de abrir ao público.

### 11. Organização do código

Módulos por feature, cada um com `api`, `aplicacao`, `dominio` e `infra`:

| Módulo | Responsabilidade |
|---|---|
| `autenticacao` | Usuário, login, segundo fator |
| `conta` | Conta, exportação, exclusão |
| `pessoa` | Marcadores |
| `carteira` | Carteiras e cartões |
| `categoria` | Lista editável |
| `compromisso` | Planos e geração de ocorrências |
| `lancamento` | Realizado, transferências, pagamento de fatura |
| `projecao` | Cálculo da linha do tempo e simulação, sem tabelas próprias |

`projecao` lê os outros módulos por interfaces públicas, sem fazer join nas tabelas deles.

### 11.1. Confiança, parte e atualização (07/11/2026)

Acrescentado pela concepção do produto (conceito "Folha": a sobra a lápis até conciliar, em tinta depois).

- **Confiança da sobra.** Cada mês carrega `FIRME` ou `ESTIMADA`. É estimada enquanto houver item `ESTIMADO` ainda previsto ou fatura cujo total calculado não bate com o total informado pelo banco. A conciliação usa o total que a pessoa informou do banco (`fatura_informada.total`, "bate com minha fatura?") ou, na falta dele, o valor com que a fatura foi paga (`pagamento_fatura.valor`). Informar não é pagar: a tabela é separada de propósito.
- **Parte.** Um compromisso pode ser só parte desta conta (`parte` de 1 a 100 por cento), para quem divide aluguel sem juntar contas. A parte entra no total antes de dividir as parcelas, com meio centavo arredondado para cima. Lançamento avulso e pagamento informado pela pessoa já são o valor real, sem parte.
- **Atualizado em.** A projeção devolve o instante mais recente entre compromissos criados, lançamentos e faturas pagas, porque uma sobra desatualizada é uma sobra falsa. Editar um compromisso não muda a data; isso é uma limitação aceita.

### 12. Tabelas

Todas com `id uuid`, `conta_id` (exceto `conta`), `criado_em` e `atualizado_em`, com constraints nomeadas e FKs indexadas.

| Tabela | Colunas principais | Restrições |
|---|---|---|
| `usuario` | `conta_id`, `email`, `senha_hash`, `totp_segredo_cifrado` | `uk_usuario_email` |
| `conta` | `nome` | |
| `pessoa` | `apelido`, `arquivada` | Apelido único por conta |
| `categoria` | `nome`, `arquivada` | Nome único por conta |
| `carteira` | `tipo`, `nome`, `saldo_inicial`, `arquivada` | `tipo` em `CONTA`, `DINHEIRO`, `VALE` |
| `cartao` | `nome`, `dia_fechamento`, `dia_vencimento`, `limite` | Dias de 1 a 31 |
| `compromisso` | `natureza`, `forma`, `certeza`, `descricao`, `valor`, `parcelas`, `data_inicio`, `data_fim`, `dia_recorrencia`, `recorrencia_anual`, `carteira_id`, `cartao_id`, `fatura_inicial`, `pessoa_id`, `categoria_id`, `parte`, `cancelado_a_partir_de` | Carteira **ou** cartão, nunca os dois; cartão até 48x; boleto até 120x; `parte` nula ou de 1 a 100 |
| `ocorrencia_ajuste` | `compromisso_id`, `numero_ocorrencia`, `valor`, `vencimento`, `fatura`, `ignorada`, `propaga` | Único por `compromisso_id` + `numero_ocorrencia` |
| `lancamento` | `tipo`, `descricao`, `valor`, `data`, `carteira_id`, `carteira_destino_id`, `compromisso_id`, `numero_ocorrencia`, `pessoa_id`, `categoria_id`, `ignorado` | Único por `compromisso_id` + `numero_ocorrencia` |
| `pagamento_fatura` | `cartao_id`, `competencia`, `valor`, `data`, `carteira_id` | Único por cartão + competência |
| `fatura_informada` | `cartao_id`, `competencia`, `total`, `informado_em` | Único por cartão + competência; só concilia, não paga |

## Consequências

- A Entrega B acrescenta formas de compromisso e o tipo `INCERTO` sem mudar o núcleo. Isso ainda precisa ser confirmado pela pesquisa de regras do consórcio.
- O motor de projeção é o coração do produto e o maior investimento em testes, com um exemplo de referência calculado à mão.
- A feature `tarefa` herdada do modelo é removida quando a primeira feature real entrar.

## Em aberto

- Comportamento exato do dia do fechamento nos bancos reais. O teste na Mobills não respondeu isso, porque a fatura pode ser escolhida à mão.
- Se a compra estornada entra na Entrega A ou fica na B. Por ora, fica na B.
- Se a linha do tempo deve mostrar também um saldo acumulado.
