## O quê

<!-- O que este PR muda, em uma ou duas frases. -->

## Por quê

<!-- Problema ou objetivo. Link da issue, se houver. -->

## Como testar

<!-- Passos para validar. Print ou log quando for visual. -->

## Checklist

- [ ] Resolve o que a descrição promete, sem mudança fora de escopo
- [ ] Testes cobrem o comportamento novo e os casos de borda (e falhariam sem a mudança)
- [ ] Contrato de API compatível, ou versionado e documentado
- [ ] Validação de entrada, autorização no nível do objeto, nenhum dado sensível em log
- [ ] Sem segredo, URL de ambiente ou credencial no código
- [ ] Migração segura (expand/contract, lock, índice) e nenhuma migração antiga editada
- [ ] Sem N+1 ou consulta sem paginação; transações com escopo correto
- [ ] Erros tratados e propagados corretamente, nada engolido
- [ ] Nomes claros, sem duplicação nova, métodos e classes de tamanho razoável
- [ ] Documentação (README, ADR) atualizada quando for o caso
- [ ] Sem código morto, comentado ou TODO sem issue
