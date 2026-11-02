# ADR 0001: Estrutura do projeto e quality gate

- **Status:** aceito
- **Data:** 2026-11-01

## Contexto

Este repositório é o modelo para todo backend novo. Precisa nascer com uma estrutura que escale de um monólito modular para microsserviços, e com validação automática que impeça código fora do padrão de entrar na `main`, sem custo de ferramentas.

## Decisão

- **Stack:** Java 25 (LTS), Spring Boot 4.1, Maven (via Maven Wrapper), PostgreSQL 18 + Flyway.
- **Organização por feature**, com camadas dentro de cada uma: `api` → `aplicacao` → `dominio` ← `infra`.
  - O domínio não depende de Spring nem de JPA. Persistência entra por uma porta (`dominio`) e um adaptador (`infra`).
  - Hexagonal só onde há integração real (banco, filas, APIs externas). Nada de interface para um service com uma única implementação.
- **API:** REST versionada pelo caminho (`/api/v1`), DTOs como `record`, validação com Bean Validation na borda, erros em Problem Details (RFC 9457) com `codigo` estável, listagens sempre paginadas (máx. 100).
- **Banco:** só o Flyway altera o schema (`ddl-auto=validate`). Migrações nomeadas por timestamp e imutáveis depois de aplicadas.
- **Quality gate**, que reprova o build:

  | Verificação | Ferramenta | Critério |
  |---|---|---|
  | Formatação | Spotless (palantir-java-format) | Qualquer diferença |
  | Testes | JUnit 5, Mockito, AssertJ, Testcontainers | Qualquer falha |
  | Cobertura | JaCoCo | < 80% linha ou < 70% branch |
  | Bugs | SpotBugs | Severidade média ou alta |
  | Code smells | PMD | Prioridade 1–2 (o resto só alerta) |
  | Segredos | Gitleaks | Qualquer segredo |
  | Dependências | Dependency Review | CVE alta ou crítica em PR |
  | Análise contínua (opcional) | SonarQube Cloud | Quality gate do Sonar |

## Consequências

- Todo projeto criado a partir deste modelo já começa com o gate ligado.
- A `main` precisa de proteção (PR obrigatório + checks verdes) para o gate valer de verdade. Em repositório público isso é gratuito.
- A cobertura do JaCoCo é global; a métrica de "código novo" fica com o Sonar quando estiver habilitado.
