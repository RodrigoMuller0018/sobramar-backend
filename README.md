# Sobramar: backend

API do **Sobramar**, que mostra quanto do futuro financeiro de uma pessoa já está comprometido e quanto sobra em cada mês. Java 25, Spring Boot 4 e PostgreSQL. O frontend está em [sobramar-frontend](https://github.com/RodrigoMuller0018/sobramar-frontend).

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

## Configuração local

Os segredos vêm do ambiente e nunca entram no Git. Copie `.env.example` para `.env`, preencha e exporte as variáveis antes de subir a aplicação. Sem `SOBRAMAR_JWT_SEGREDO` e `SOBRAMAR_CHAVE_CIFRAGEM`, ela não sobe, de propósito. Os testes geram chaves próprias a cada execução.

Não há cadastro público na Entrega A: a conta é criada ao subir, a partir de `SOBRAMAR_CONTA_INICIAL_EMAIL` e `SOBRAMAR_CONTA_INICIAL_SENHA`.

## Comandos

| Comando | O que faz |
|---|---|
| `./mvnw spring-boot:run` | Sobe a aplicação. O PostgreSQL do `compose.yaml` sobe junto, sozinho |
| `./mvnw spotless:apply` | Formata o código no padrão (rode antes de commitar) |
| `./mvnw verify` | Roda **todo** o quality gate localmente, igual ao CI |
| `./mvnw test` | Só os testes |

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

Relatório de cobertura: `target/site/jacoco/index.html`.

## Fluxo de trabalho

- Branches curtas: `feat/...`, `fix/...`, `chore/...`
- Commits no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/) (`feat:`, `fix:`, `test:`...)
- Tudo entra na `main` via PR com o checklist do template preenchido e o CI verde
- Decisões relevantes viram um ADR em `docs/adr/`
