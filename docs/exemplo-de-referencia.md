# Exemplo de referência

Uma família fictícia com a conta **feita à mão**. O teste `ExemploDeReferenciaTest` confere o sistema contra esta tabela. Se um dia os dois divergirem, um deles está errado, e isso precisa ser explicado antes de seguir.

Todos os dados são inventados. Período: janeiro a dezembro de 2027. Cartão: fecha dia 25, vence dia 5.

## Entradas

| Item | Valor | Quando |
|---|---|---|
| Salário | R$ 5.000,00 | Todo dia 5 |
| 13º, primeira parte | R$ 2.500,00 | 20/11 |
| 13º, segunda parte | R$ 2.500,00 | 20/12 |
| Crédito de vale | R$ 800,00 | Todo dia 1, em carteira de vale |

## Saídas

| Item | Valor | Como cai |
|---|---|---|
| Aluguel | R$ 1.500,00 | Todo dia 10. Em janeiro, o pago de verdade foi R$ 1.550,00 |
| Luz (estimada) | R$ 180,00 | Todo dia 15. **Fevereiro foi ignorado** |
| Notebook no cartão | R$ 3.600,00 em 12x | Compra em 10/12/2026: fecha em 25/12, então vence de 05/01 a 05/12/2027. R$ 300,00 por mês |
| Geladeira no cartão | R$ 1.200,00 em 6x | Compra em 25/03, **dia do fechamento**: fecha em 25/04, então vence de 05/05 a 05/10. R$ 200,00 por mês |
| Assinatura no cartão | R$ 39,90 | Cobrança dia 20, a partir de 20/01: vence a partir de 05/02 |
| Curso em boleto | R$ 3.600,00 em 36x | R$ 100,00 todo dia 10, a partir de 10/02 |
| IPVA | R$ 1.200,00 | 15/03, uma vez |
| Almoço avulso | R$ 50,00 | 12/01 |

## O que não pode entrar na sobra

| Item | Por quê |
|---|---|
| Transferência de R$ 200,00 entre contas em 13/01 | É só o dinheiro mudando de lugar |
| Pagamento da fatura de janeiro | As compras já foram contadas nas parcelas |
| Crédito de vale e gasto de R$ 30,00 no vale em 14/01 | O vale tem bloco próprio |

## Resultado esperado

| Mês | Receitas | Compromissos | Sobra | Conta |
|---|---|---|---|---|
| Jan | 5.000,00 | 2.080,00 | **2.920,00** | 1.550 + 180 + 300 + 50 |
| Fev | 5.000,00 | 1.939,90 | **3.060,10** | 1.500 + 300 + 39,90 + 100 |
| Mar | 5.000,00 | 3.319,90 | **1.680,10** | 1.500 + 180 + 300 + 39,90 + 100 + 1.200 |
| Abr | 5.000,00 | 2.119,90 | **2.880,10** | 1.500 + 180 + 300 + 39,90 + 100 |
| Mai a Out | 5.000,00 | 2.319,90 | **2.680,10** | Abril + 200 da geladeira |
| Nov | 7.500,00 | 2.119,90 | **5.380,10** | Abril, com o 13º |
| Dez | 7.500,00 | 2.119,90 | **5.380,10** | Abril, com o 13º |

Em todos os meses, o bloco de vale mostra R$ 800,00 de crédito, e janeiro mostra também R$ 30,00 de gasto.
