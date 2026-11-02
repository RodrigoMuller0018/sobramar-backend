# Contrato da API: Entrega A

- **Status:** proposto, ainda sem implementação
- **Data:** 2026-11-02
- **Modelo:** [ADR 0002](../adr/0002-modelo-de-dominio-financeiro.md)

Este documento é a fonte do contrato até a implementação. Depois, o OpenAPI gerado pelo código passa a ser a referência e é validado no CI. Mudar o contrato significa mudar este arquivo primeiro.

## Convenções

| Tema | Regra |
|---|---|
| Base | `/api/v1`, em JSON, com campos em `camelCase` |
| Acesso | Tudo exige sessão, exceto `POST /autenticacao/entrar` e `POST /autenticacao/segundo-fator` |
| Conta | Vem da sessão. **Nunca** vai na URL ou no corpo |
| Dinheiro | Inteiro em centavos: `valorCentavos: 12990` = R$ 129,90 |
| Data | `2027-03-10` |
| Mês | `2027-03` |
| Instante | ISO-8601 em UTC |
| IDs | UUID |
| Listas | `page` e `size` (padrão 20, máximo 100), no formato `{ content, page }` |
| Criação | `201` com `Location` |
| Sem corpo | `204` |

Campos desconhecidos no corpo são ignorados. Acrescentar campo opcional não quebra o contrato.

## Erros

Problem Details (RFC 9457), com `codigo` estável. O front mapeia `codigo` para texto e nunca exibe `detail`.

```json
{
  "type": "about:blank",
  "title": "Regra de negócio violada",
  "status": 422,
  "detail": "Mensagem técnica",
  "codigo": "CARTAO_LIMITE_PARCELAS"
}
```

| Status | Quando | `codigo` |
|---|---|---|
| 400 | Formato ou validação de campo | Sem código de negócio |
| 401 | Sem sessão, login ou segundo fator inválidos | `NAO_AUTENTICADO`, `CREDENCIAIS_INVALIDAS` |
| 404 | Inexistente **ou de outra conta** | `RECURSO_NAO_ENCONTRADO` |
| 409 | Conflito de estado | `OCORRENCIA_JA_PAGA`, `FATURA_JA_PAGA`, `APELIDO_DUPLICADO`, `CATEGORIA_DUPLICADA` |
| 422 | Regra violada | Ver abaixo |
| 429 | Excesso de tentativas | `MUITAS_TENTATIVAS`, com `Retry-After` |

Códigos de 422: `PARCELAS_INVALIDAS`, `CARTAO_LIMITE_PARCELAS`, `BOLETO_LIMITE_PARCELAS`, `VALOR_INVALIDO`, `DIA_INVALIDO`, `ORIGEM_OBRIGATORIA`, `ORIGEM_DUPLA`, `CARTEIRA_INCOMPATIVEL`, `PERIODO_INVALIDO`, `CONFIRMACAO_INVALIDA`.

## Autenticação

| Método | Rota | Corpo | Resposta |
|---|---|---|---|
| POST | `/autenticacao/entrar` | `{ email, senha }` | `200 { segundoFatorObrigatorio: true }` |
| POST | `/autenticacao/segundo-fator` | `{ codigo }` | `204`, define os cookies de sessão |
| POST | `/autenticacao/renovar` | | `204`, rotaciona o refresh token |
| POST | `/autenticacao/sair` | | `204`, encerra a sessão |
| GET | `/autenticacao/eu` | | `200 { email, nomeConta }` |
| POST | `/autenticacao/segundo-fator/ativacao` | | `200 { uriOtpauth }` |
| POST | `/autenticacao/segundo-fator/ativacao/confirmar` | `{ codigo }` | `204` |

E-mail ou senha errados geram sempre o mesmo `401`, sem dizer qual dos dois falhou.

## Cadastros

Mesmo padrão nos quatro recursos abaixo:

| Método | Rota | Resposta |
|---|---|---|
| GET | `/{recurso}?arquivadas=false` | `200`, paginado |
| POST | `/{recurso}` | `201` |
| GET | `/{recurso}/{id}` | `200` |
| PUT | `/{recurso}/{id}` | `200` |
| DELETE | `/{recurso}/{id}` | `204`. Arquiva, para preservar o histórico |

| Recurso | Corpo |
|---|---|
| `/pessoas` | `{ "apelido": "Ana" }` (até 40 caracteres) |
| `/categorias` | `{ "nome": "Moradia" }` (até 40 caracteres) |
| `/carteiras` | `{ "tipo": "CONTA", "nome": "Conta principal", "saldoInicialCentavos": 150000 }` (`CONTA`, `DINHEIRO` ou `VALE`) |
| `/cartoes` | `{ "nome": "Cartão azul", "diaFechamento": 25, "diaVencimento": 5, "limiteCentavos": 500000 }` (limite opcional) |

As respostas repetem o corpo, com `id`, `arquivada` (`arquivado` no cartão) e `criadoEm`. A resposta de cartão inclui também `limiteUsadoCentavos` e `limiteDisponivelCentavos`, com todas as parcelas ainda não pagas somadas.

## Compromissos

| Método | Rota | Resposta |
|---|---|---|
| GET | `/compromissos?natureza=&forma=&pessoaId=&ativos=true` | `200`, paginado |
| POST | `/compromissos` | `201` |
| GET | `/compromissos/{id}` | `200`, com as ocorrências calculadas |
| PUT | `/compromissos/{id}` | `200`. Altera o plano inteiro; ocorrências pagas mantêm o valor real |
| PATCH | `/compromissos/{id}/ocorrencias/{numero}` | `200`. Edita uma parcela, com escopo (ver abaixo) |
| POST | `/compromissos/{id}/cancelamento` | `200`. Corpo: `{ "aPartirDe": "2027-06-01" }` |
| DELETE | `/compromissos/{id}` | `204`. Só sem ocorrências pagas; caso contrário `409` |

### Exemplos de criação

**Compra no cartão em 12x**

```json
{
  "natureza": "SAIDA",
  "forma": "PARCELADO",
  "certeza": "CERTO",
  "descricao": "Notebook",
  "valorTotalCentavos": 480000,
  "parcelas": 12,
  "dataInicio": "2027-01-15",
  "cartaoId": "uuid",
  "pessoaId": null,
  "categoriaId": "uuid"
}
```

**Boleto em 36x**

Igual ao exemplo acima, com `parcelas: 36` e `carteiraId` no lugar de `cartaoId`. `dataInicio` é o primeiro vencimento, e os demais caem no mesmo dia dos meses seguintes.

**Conta recorrente com valor estimado**

```json
{
  "natureza": "SAIDA",
  "forma": "RECORRENTE",
  "certeza": "ESTIMADO",
  "descricao": "Luz",
  "valorCentavos": 18000,
  "dataInicio": "2027-01-10",
  "dataFim": null,
  "carteiraId": "uuid"
}
```

**Salário**

Igual ao recorrente, com `natureza: "ENTRADA"` e `certeza: "CERTO"`.

**13º em duas parcelas**

Dois compromissos com `forma: "EVENTO"` e `natureza: "ENTRADA"`, cada um com `valorCentavos` e `dataInicio` do mês escolhido.

**IPVA**

`forma: "EVENTO"`, `natureza: "SAIDA"`, com `recorrenciaAnual: true` para repetir todo ano.

**Crédito mensal de vale**

Recorrente de `ENTRADA` com `carteiraId` de uma carteira `VALE`.

### Escolher a fatura

Em compra no cartão, `faturaInicial` é opcional. Sem ela, o sistema calcula. Com ela, a escolha é respeitada, e a resposta traz um aviso se não combinar com a data:

```json
{
  "id": "uuid",
  "faturaInicial": "2026-11",
  "avisos": [{ "codigo": "FATURA_INCOMPATIVEL_COM_DATA" }]
}
```

`GET /cartoes/{id}/fatura-sugerida?dataCompra=2026-11-26` devolve `{ "competencia": "2026-12", "fechamento": "2026-11-25", "vencimento": "2026-12-05" }`, para o formulário mostrar a fatura antes de salvar.

### Editar uma parcela

```json
{
  "escopo": "SOMENTE_ESTA",
  "valorCentavos": 12000,
  "vencimento": "2027-03-10",
  "fatura": null,
  "ignorada": false
}
```

`escopo` aceita `SOMENTE_ESTA`, `ESTA_E_FUTURAS` e `TODAS`. Só os campos enviados mudam. Editar parcela já paga retorna `OCORRENCIA_JA_PAGA`. Item com `ignorada: true` continua na lista, mas sai dos totais.

### Regras

- Exatamente um entre `carteiraId` e `cartaoId`: `ORIGEM_OBRIGATORIA` ou `ORIGEM_DUPLA`.
- Cartão: de 1 a 48 parcelas. Boleto: de 1 a 120.
- Entrada não aceita `cartaoId`: `CARTEIRA_INCOMPATIVEL`.
- Referências de outra conta respondem `404`.

## Lançamentos

| Método | Rota | Resposta |
|---|---|---|
| GET | `/lancamentos?de=&ate=&carteiraId=&pessoaId=&categoriaId=` | `200`, paginado |
| POST | `/lancamentos` | `201` |
| PUT | `/lancamentos/{id}` | `200` |
| DELETE | `/lancamentos/{id}` | `204` |
| POST | `/compromissos/{id}/ocorrencias/{numero}/pagamento` | `201`. Corpo: `{ "data", "valorCentavos", "carteiraId" }` |
| DELETE | `/compromissos/{id}/ocorrencias/{numero}/pagamento` | `204` |
| POST | `/cartoes/{id}/faturas/{competencia}/pagamento` | `201`. Corpo: `{ "data", "valorCentavos", "carteiraId" }` |
| DELETE | `/cartoes/{id}/faturas/{competencia}/pagamento` | `204` |

**Gasto avulso**

```json
{
  "tipo": "GASTO",
  "descricao": "Almoço",
  "valorCentavos": 3500,
  "data": "2027-01-12",
  "carteiraId": "uuid",
  "pessoaId": null,
  "categoriaId": "uuid"
}
```

`tipo` aceita `GASTO`, `RECEITA` e `TRANSFERENCIA`. O campo opcional `ignorado: true` tira o lançamento dos totais sem apagá-lo. A transferência exige `carteiraDestinoId` diferente da origem, e não entra na sobra.

## Projeção

### `GET /projecao?de=2027-01&ate=2027-12&pessoaId=&categoriaId=`

O período tem no máximo 24 meses, e passar disso retorna `PERIODO_INVALIDO`.

```json
{
  "meses": [
    {
      "competencia": "2027-03",
      "receitasCentavos": 800000,
      "compromissosCentavos": 523000,
      "sobraCentavos": 277000,
      "contemEstimativa": true,
      "faturas": [
        {
          "cartaoId": "uuid",
          "nomeCartao": "Cartão azul",
          "vencimento": "2027-03-05",
          "totalCentavos": 120000,
          "paga": false
        }
      ],
      "itens": [
        {
          "compromissoId": "uuid",
          "numeroOcorrencia": 3,
          "totalOcorrencias": 12,
          "descricao": "Notebook",
          "natureza": "SAIDA",
          "certeza": "CERTO",
          "valorCentavos": 40000,
          "dataCompra": "2027-01-15",
          "vencimento": "2027-03-05",
          "ignorada": false,
          "cartaoId": "uuid",
          "carteiraId": null,
          "pessoaId": null,
          "categoriaId": "uuid",
          "situacao": "PREVISTO"
        }
      ],
      "vale": [
        {
          "carteiraId": "uuid",
          "nome": "Vale-refeição",
          "creditosCentavos": 90000,
          "gastosCentavos": 40000
        }
      ]
    }
  ]
}
```

- `situacao`: `PREVISTO` ou `REALIZADO`. Com `REALIZADO`, `valorCentavos` é o valor real.
- `sobraCentavos` = `receitasCentavos` − `compromissosCentavos`. **Pode ser negativa**, e o front mostra isso sem tom de alarme.
- `vale` nunca entra na sobra.
- Com filtro por pessoa, os totais consideram só os itens dela.
- Gastos avulsos entram em `itens`, sem `compromissoId`.

### `POST /projecao/simulacao`

Mesma conta da projeção, com uma compra hipotética. **Nada é gravado.**

```json
{
  "de": "2027-01",
  "ate": "2027-12",
  "compra": {
    "valorTotalCentavos": 360000,
    "parcelas": 36,
    "dataInicio": "2027-02-10",
    "cartaoId": null,
    "carteiraId": "uuid"
  }
}
```

```json
{
  "meses": [
    {
      "competencia": "2027-03",
      "sobraAtualCentavos": 277000,
      "sobraSimuladaCentavos": 267000,
      "diferencaCentavos": -10000
    }
  ],
  "valorParcelaCentavos": 10000,
  "primeiroPagamento": "2027-02-10",
  "ultimoPagamento": "2030-01-10"
}
```

`ultimoPagamento` responde "quando a terra volta", mesmo fora do período pedido. As regras de validação são as de criação de compromisso. Confirmar a compra é um `POST /compromissos` normal.

## Pendências

### `GET /pendencias?dias=7`

Faturas e parcelas não pagas, vencidas ou que vencem nos próximos `dias` (padrão 7, máximo 31). É calculado, sem tabela própria, e usa as mesmas regras da projeção. Por isso, o número é o mesmo em qualquer tela.

```json
{
  "itens": [
    {
      "tipo": "FATURA",
      "descricao": "Cartão azul",
      "vencimento": "2027-03-05",
      "diasAteVencimento": 3,
      "valorCentavos": 120000,
      "cartaoId": "uuid",
      "competencia": "2027-03",
      "compromissoId": null,
      "numeroOcorrencia": null
    }
  ]
}
```

`tipo` aceita `FATURA` e `OCORRENCIA`. `diasAteVencimento` é negativo em atraso. Confirmar o pagamento usa as rotas de pagamento acima. Na Entrega A não há notificação externa.

Esconder os valores da tela é só uma preferência do front, sem rota na API.

## Conta

| Método | Rota | Corpo | Resposta |
|---|---|---|---|
| GET | `/conta/exportacao?formato=json` | | `200`, com todos os dados da conta (`json` ou `csv`). Exige reautenticação recente |
| DELETE | `/conta` | `{ "senha", "codigoSegundoFator", "confirmacao": "EXCLUIR" }` | `204`, apaga todos os dados da conta |

Confirmação incorreta retorna `CONFIRMACAO_INVALIDA`.

## Fora da Entrega A

- Cadastro público e recuperação de conta (Entrega C).
- Importação OFX/CSV, consórcio, financiamento, investimentos e estorno (Entrega B).
- Chave de idempotência em `POST`. Na Entrega A há um único usuário e o front bloqueia o duplo clique; a Entrega C reavalia.
