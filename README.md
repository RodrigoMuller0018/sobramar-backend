# template-backend-spring

Modelo de backend **Java 25 + Spring Boot 4 + PostgreSQL**, com um quality gate que roda a cada PR. Clique em **Use this template** para criar um projeto novo já no padrão.

## O que já vem pronto

- **Estrutura por feature** (`api` → `aplicacao` → `dominio` ← `infra`), com uma feature de exemplo (`tarefa`) mostrando o padrão completo.
- **API REST** versionada (`/api/v1`), DTOs como `record`, validação na borda, erros em Problem Details (RFC 9457) e paginação obrigatória.
- **Flyway + PostgreSQL 18**, com o Hibernate só validando o schema.
- **Testes:** unitário (JUnit 5 + Mockito + AssertJ), de API (`@WebMvcTest`) e de integração com PostgreSQL real (Testcontainers).
- **Quality gate** no `./mvnw verify` e no GitHub Actions. Veja a tabela completa em [docs/adr/0001](docs/adr/0001-estrutura-e-quality-gate.md).

## Pré-requisitos

- JDK 25
- Docker (o PostgreSQL local e os testes de integração rodam em containers)

O Maven não precisa estar instalado: use o `./mvnw`, que já vem no projeto.

## Comandos

| Comando | O que faz |
|---|---|
| `./mvnw spring-boot:run` | Sobe a aplicação. O PostgreSQL do `compose.yaml` sobe junto, sozinho |
| `./mvnw spotless:apply` | Formata o código no padrão (rode antes de commitar) |
| `./mvnw verify` | Roda **todo** o quality gate localmente, igual ao CI |
| `./mvnw test` | Só os testes |

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

Relatório de cobertura: `target/site/jacoco/index.html`.

## Depois de criar um projeto a partir deste modelo

1. **Renomeie** o `artifactId`/`name` no `pom.xml` e o pacote `io.github.rodrigomuller0018.template`.
2. **Apague a feature `tarefa`** (código, testes e a migração) quando criar a primeira feature real. Use-a como referência antes disso.
3. **Ligue o Dependency graph:** *Settings → Advanced Security → Dependabot alerts → Enable* (liga o grafo junto). Sem ele, o check `Dependências vulneráveis` falha em todo PR com "Dependency review is not supported on this repository".
4. **Proteja a `main`:** *Settings → Rules → Rulesets → New branch ruleset*, alvo `main`, marcando:
   - *Require a pull request before merging*
   - *Require status checks to pass* → adicione `Build, testes e análise estática`, `Segredos no código` e `Dependências vulneráveis`
   - *Block force pushes*
5. **SonarQube Cloud (opcional, grátis em repositório público):**
   1. Entre em [sonarcloud.io](https://sonarcloud.io) com o GitHub e importe o repositório.
   2. Em *Settings → Secrets and variables → Actions*:
      - Secret `SONAR_TOKEN`: o token gerado no Sonar
      - Variables `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY`: os valores que o Sonar mostrar
   3. No Sonar, desative a *Automatic Analysis* (a análise passa a vir do CI, com cobertura).

## Fluxo de trabalho

- Branches curtas: `feat/...`, `fix/...`, `chore/...`
- Commits no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/) (`feat:`, `fix:`, `test:`...)
- Tudo entra na `main` via PR com o checklist do template preenchido e o CI verde
- Decisões relevantes viram um ADR em `docs/adr/`
