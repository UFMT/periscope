# Modernização do Periscope

Plano de atualização tecnológica do sistema Periscope (UFMT), **100% concluído**. Todas as fases
foram implementadas e a camada legada JSF/PrimeFaces foi descomissionada.

## Decisões arquiteturais (consolidadas)

| Decisão | Escolha |
|---------|---------|
| Linguagem | **Java 21 LTS** |
| Servidor de aplicação | **WildFly 34.0.1.Final** (Jakarta EE 10) |
| Persistência | **MongoDB 7** (local smoke: 4.4+) + driver 5.2.1 + **Morphia 2.5.3** |
| Frontend | **SPA React 19** (`periscope-ui`, i18n `pt-BR`/`en-US`) como interface única + API REST (JSF/PrimeFaces removido) |
| Segurança | Cookie `HttpOnly` (`periscope_token`) + JWT HS256 com revogação por `jti`, fail-fast em produção, autorização por projeto em `/files` e `mustChangePassword` |
| Empacotamento | **WAR único** (`periscope-web` + EJB e SPA embarcados no `Dockerfile` multi-stage) |
| Homologação / Produção | [`docker-compose.prod.yml`](../../docker-compose.prod.yml) (Mongo auth + Caddy TLS) e workflow [`.github/workflows/deploy-staging.yml`](../../.github/workflows/deploy-staging.yml) |

## Estado atual

- **WAR único** `periscope-web/target/periscope.war`, context-root `/periscope`, WildFly 34 (JDK 21), redirecionando `/periscope/` → `/periscope/app/`
- **Dockerfile de 3 estágios** (`node:22-alpine` → `maven:3.9-eclipse-temurin-21` → `quay.io/wildfly/wildfly:34.0.1.Final-jdk21`) com SPA embarcada, OpenAPI e `HEALTHCHECK`
- **API REST** em `/periscope/rest` com paridade funcional completa (auth/refresh/change-password, referência, projetos + observadores, patentes manual/edição/GridFS, import, harmonização entidades/regras/undo, 13 relatórios + filtros + CSV, CRUD de usuários, arquivos por projeto, health)
- **OpenAPI 3** em `/openapi` (MicroProfile OpenAPI / SmallRye no WildFly)
- **SPA React** única em `/periscope/app/` (`pt-BR` / `en-US`)
- **CI/CD** `.github/workflows/ci.yml`: `mvn -B clean verify`, IT `-Pit`, frontend lint + Vitest + build, dependency-check (`failBuildOnCVSS=7`) e job E2E real com Docker (`mongo:7` + WildFly 34 + Playwright) em push/PR/`workflow_dispatch`
- **Testes:** 66 unitários Maven (`29` EJB + `37` Web) + ITs Testcontainers (perfil `it`) + 7 arquivos / 10 testes Vitest no frontend + 6 specs Playwright E2E com auto-seed

## Fases e documentos

| Fase | Documento | Objetivo | Estado |
|------|-----------|----------|--------|
| 0 | [fase-00-preparacao.md](fase-00-preparacao.md) | Baseline, inventário, ambiente local | Concluída |
| 1 | [fase-01-build.md](fase-01-build.md) | Compilar com Java 21 e Maven moderno | Concluída |
| 2 | [fase-02-jakarta-ee.md](fase-02-jakarta-ee.md) | `javax.*` → `jakarta.*`, CDI moderno | Concluída |
| 3 | [fase-03-mongodb.md](fase-03-mongodb.md) | Driver 5.x, Morphia 2, GridFS, agregações | Concluída |
| 4 | [fase-04-lucene.md](fase-04-lucene.md) | Lucene 9.x, harmonização Fast-Join | Concluída |
| 5 | [fase-05-bibliotecas.md](fase-05-bibliotecas.md) | PDFBox 3, POI 5 | Concluída |
| 6 | [fase-06-war-wildfly.md](fase-06-war-wildfly.md) | WAR único, WildFly 34, Docker 3 estágios & Compose dev/prod | Concluída |
| 7 | [fase-07-spa.md](fase-07-spa.md) | API REST + SPA React (paridade 100%, i18n, JSF removido) | Concluída |
| 8a / 8b | [fase-08-qualidade-ci.md](fase-08-qualidade-ci.md) | Testes (Maven + Vitest + E2E), CI Docker, OpenAPI, staging | Concluída |

Referência de homologação: [ambiente-homologacao-futuro.md](ambiente-homologacao-futuro.md).
