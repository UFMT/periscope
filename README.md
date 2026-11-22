# Periscope

> **Status do fork: modernização 100% concluída.**  
> O backend roda em **Java 21 / Jakarta EE 10 / WildFly 34**, a **SPA React (`periscope-ui`)**
> possui paridade funcional completa e é a única interface de usuário (embutida automaticamente
> no WAR pelo `Dockerfile`), a camada legada JSF/PrimeFaces foi descomissionada, a segurança foi
> endurecida para produção e a suíte automatizada cobre backend, frontend e E2E Docker no CI.

Sistema web de **análise e harmonização de patentes** (UFMT). Persistência em **MongoDB**
(Morphia 2.5); harmonização de nomes com **Lucene 9** (Fast-Join).

| Ponto de acesso | URL | Observação |
|-----------------|-----|------------|
| **SPA React** (`periscope-ui`) | http://localhost:8080/periscope/app/ | Interface única do usuário (`pt-BR` / `en-US`) |
| **Raiz da aplicação** | http://localhost:8080/periscope/ | Redireciona para `/periscope/app/` |
| **API REST** | http://localhost:8080/periscope/rest/ | JAX-RS (JWT HS256 / cookie HttpOnly) |
| **OpenAPI** | http://localhost:8080/openapi | MicroProfile OpenAPI (SmallRye) |
| **Health check** | http://localhost:8080/periscope/rest/health | MongoDB + índice Lucene (`UP`/`DOWN`) |

Login padrão de desenvolvimento: `admin` / `123456` (seed no primeiro deploy; configurável
via variáveis de ambiente em produção).

Ver também: [CHANGELOG.md](CHANGELOG.md) · [docs/modernization/](docs/modernization/README.md) ·
[periscope-ui/README.md](periscope-ui/README.md)

## Status da modernização (100%)

1. **Paridade funcional da API REST e SPA React:** todos os 13 relatórios com filtros,
   descrições de seções IPC e exportação CSV; CRUD completo de usuários (`ADMIN`); criação
   manual e edição completa de patentes (incluindo prioridades) e upload/download GridFS na UI;
   gestão de observadores de projeto; criação, edição, undo, país/estado/natureza e navegação de
   entidades na harmonização; internacionalização completa em português (`pt-BR`) e inglês
   (`en-US`).
2. **Empacotamento e entrega da SPA no Docker:** `Dockerfile` multi-stage de 3 estágios
   (`node:22-alpine` → `maven:3.9-eclipse-temurin-21` com SPA embutida em `webapp/app/` →
   `quay.io/wildfly/wildfly:34.0.1.Final-jdk21` com OpenAPI e `HEALTHCHECK`), além de redirecionamento
   automático de `/periscope/` para `/periscope/app/`.
3. **Descomissionamento e limpeza do legado:** remoção completa de JSF, PrimeFaces, tema
   `bootstrap`, repositórios `tools/legacy-m2/`, `.xhtml`, `faces-config.xml`, beans/conversores
   legados, `Capfile`, `config/deploy.rb`, descritores Eclipse e scripts obsoletos.
4. **Segurança e prontidão para produção:** controle de acesso por projeto em `GET /rest/files/{id}`
   (correção de IDOR), JWT via Jackson com validação de algoritmo e `jti` revogável em
   `/auth/logout` e `/auth/refresh`, cookie `HttpOnly` `SameSite=Lax` (sem JWT no
   `localStorage`), fail-fast em `PERISCOPE_PRODUCTION=true` para `PERISCOPE_JWT_SECRET`,
   seed administrativo configurável com `mustChangePassword` e modal embutido +
   `POST /auth/change-password`, além de [`docker-compose.prod.yml`](docker-compose.prod.yml)
   (MongoDB 7 autenticado em rede interna + proxy Caddy TLS em [`deploy/Caddyfile`](deploy/Caddyfile))
   e [`.env.example`](.env.example).
5. **Testes, CI/CD e homologação:** 66 testes unitários Maven (JUnit 5 + Mockito + AssertJ),
   testes de integração Testcontainers (`-Pit`), testes unitários/de componentes no frontend com
   Vitest + Testing Library (7 arquivos / 10 testes), 6 specs Playwright E2E com auto-seed,
   job `e2e` real com containers Docker no CI (`.github/workflows/ci.yml`), `failBuildOnCVSS=7`
   no OWASP dependency-check e workflow operacional `.github/workflows/deploy-staging.yml`.

## Stack atual

| Componente | Versão / detalhe |
|------------|------------------|
| Java | 21 |
| Jakarta EE | 10 |
| Servidor | WildFly 34.0.1.Final |
| Frontend | React 19 + Vite + TypeScript + Recharts (`periscope-ui`, i18n `pt-BR`/`en-US`) |
| Persistência | MongoDB 7 + Morphia **2.5.3** + mongodb-driver-sync **5.2.1** |
| Harmonização | Apache Lucene **9.12.0** (Fast-Join) |
| PDF / planilhas | Apache PDFBox **3.0.3** · Apache POI **5.3.0** |
| Empacotamento | **WAR único** (`periscope-web` + EJB e SPA embarcados) |
| API | JAX-RS em `/periscope/rest` (JWT HS256 / cookie HttpOnly) |
| Docs da API | OpenAPI 3 em `/openapi` |

### Módulos

```
periscope/                 # POM pai
├── periscope-ejb/         # EJB JAR (domínio, Morphia, Lucene, importadores, relatórios)
├── periscope-web/         # WAR único (API REST + SPA embarcada em webapp/app/)
└── periscope-ui/          # Frontend React + Vitest + Playwright E2E
```

Artefato de deploy: `periscope-web/target/periscope.war`.

## Requisitos

**Opção A — Docker:** Docker e Docker Compose.

**Opção B — Manual:** JDK 21, Maven 3.9+, Node 22+ (para buildar a SPA), MongoDB 4.4+
(7.x recomendado) e WildFly 34.0.1.Final (JDK 21).

## Quick start (Docker Compose)

```bash
./scripts/dev-up.sh
# ou: docker compose up --build -d
```

O `Dockerfile` executa o estágio Node para compilar a SPA, embute em `webapp/app/`, empacota o WAR
com Maven e sobe o WildFly 34 com MicroProfile OpenAPI habilitado e `HEALTHCHECK` automático.

| Recurso | URL | Observação |
|---------|-----|------------|
| SPA React | http://localhost:8080/periscope/app/ | Embutida no container |
| Redirect raiz | http://localhost:8080/periscope/ | Redireciona para `/periscope/app/` |
| API REST | http://localhost:8080/periscope/rest/ | JWT Bearer / cookie HttpOnly |
| OpenAPI | http://localhost:8080/openapi | MicroProfile OpenAPI |
| Health | http://localhost:8080/periscope/rest/health | MongoDB + Lucene |

Login de desenvolvimento: `admin` / `123456`. Parar: `./scripts/dev-down.sh`.

Serviços: `mongodb` (`mongo:7`, sem autenticação no Compose local) e `periscope`
(WildFly 34 + WAR).

## Deploy de produção (`docker-compose.prod.yml`)

Para produção, utilize [`docker-compose.prod.yml`](docker-compose.prod.yml) com arquivo `.env`
baseado em [`.env.example`](.env.example):

```bash
cp .env.example .env
# Edite MONGO_INITDB_ROOT_*, PERISCOPE_JWT_SECRET (>=32 bytes), PERISCOPE_ADMIN_PASSWORD, PERISCOPE_DOMAIN e PERISCOPE_CORS_ORIGINS
docker compose -f docker-compose.prod.yml up --build -d
```

- **Banco e rede:** MongoDB 7 com autenticação habilitada em rede Docker `internal` (sem portas
  expostas diretamente).
- **Segurança da API:** `PERISCOPE_PRODUCTION=true` exige `PERISCOPE_JWT_SECRET` com pelo menos
  32 bytes (fail-fast se ausente ou fraco) e ativa cookie `Secure` (`PERISCOPE_COOKIE_SECURE=true`).
- **Bootstrap administrativo:** `PERISCOPE_ADMIN_*` e `PERISCOPE_FORCE_ADMIN_PASSWORD_CHANGE=true`
  configuram o usuário inicial e exigem troca obrigatória de senha no primeiro login.
- **TLS:** proxy reverso Caddy 2 com terminação HTTPS e cabeçalhos de segurança configurados em
  [`deploy/Caddyfile`](deploy/Caddyfile).

## Build e deploy manual (WildFly 34)

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # ajuste se necessário

# 1) Compilar e embarcar a SPA no WAR
cd periscope-ui && npm ci && npm run build:war && cd ..

# 2) Compilar, testar e empacotar o WAR
mvn clean package
# → periscope-web/target/periscope.war

# 3) Índice Lucene
mkdir -p /opt/periscope

# 4) WildFly 34
unset JBOSS_HOME
export JBOSS_HOME=/opt/jboss/wildfly-34.0.1.Final
cd "$JBOSS_HOME" && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./bin/standalone.sh -b 0.0.0.0

# Em outro terminal — OpenAPI (uma vez por instalação, com servidor parado) e deploy
JBOSS_HOME=/opt/jboss/wildfly-34.0.1.Final ./tools/enable-wildfly-openapi.sh
cp periscope-web/target/periscope.war \
   /opt/jboss/wildfly-34.0.1.Final/standalone/deployments/
```

Sucesso: marcador `periscope.war.deployed`. Acesse `http://localhost:8080/periscope/` (que
redireciona para `/periscope/app/`).

## Variáveis de ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `MONGODB_URI` | `mongodb://localhost:27017` | URI do MongoDB |
| `MONGODB_DATABASE` | `Periscope` | Nome do banco |
| `PERISCOPE_DIR` | `/opt/periscope` | Diretório do índice Lucene (deve ser gravável) |
| `PERISCOPE_PRODUCTION` | `false` | Se `true`, exige `PERISCOPE_JWT_SECRET` configurado e ≥ 32 bytes |
| `PERISCOPE_COOKIE_SECURE` | `false` (`true` em HTTPS) | Adiciona flag `Secure` no cookie HttpOnly de sessão |
| `PERISCOPE_JWT_SECRET` | *(efêmero em dev)* | Segredo HS256 (≥ 32 bytes; obrigatório em produção) |
| `PERISCOPE_JWT_EXPIRATION_HOURS` | `8` | Validade do JWT |
| `PERISCOPE_CORS_ORIGINS` | `http://localhost:5173` | Origins CORS permitidas (separadas por vírgula) |
| `PERISCOPE_ADMIN_USERNAME` | `admin` | Username do admin inicial criado pelo seed |
| `PERISCOPE_ADMIN_PASSWORD` | `123456` | Senha inicial do admin no primeiro seed |
| `PERISCOPE_ADMIN_EMAIL` | `admin@ufmt.br` | E-mail inicial do admin no primeiro seed |
| `PERISCOPE_FORCE_ADMIN_PASSWORD_CHANGE` | `false` (`true` no prod Compose) | Exige troca de senha obrigatória no primeiro acesso do admin |

No Docker Compose, `MONGODB_URI` aponta para o serviço `mongodb`.

## API REST

Base: `http://localhost:8080/periscope/rest`

| Área | Endpoints (resumo) |
|------|--------------------|
| Auth | `POST /auth/login`, `GET /auth/me`, `POST /auth/refresh`, `POST /auth/change-password`, `POST /auth/logout` |
| Referência | `GET /reference/countries`, `GET /reference/applicant-types`, `GET /reference/users` |
| Projetos | CRUD `/projects`, observadores `POST/DELETE /projects/{id}/observers/{userId}` |
| Patentes | `/projects/{id}/patents` (`GET`/`POST`), `/patents/{id}` (`GET`/`PUT`/`DELETE`), import multipart |
| Harmonização | Sugestões Fast-Join, entidades (`GET /entities`), regras (`POST`/`PUT /rules/{ruleId}`/`POST /rules/{ruleId}/undo`/apply) |
| Relatórios | Todos os 13 relatórios + filtros + autocomplete (`/reports/applicants`, `/reports/inventors`) + `docs-national-priority` |
| Usuários | CRUD completo `/users` (`ADMIN`) |
| Arquivos | GridFS upload/download (`/files`) autorizado por projeto |
| Health | `GET /health` → `{"status":"UP",...}` |

Autenticação via `Authorization: Bearer <token>` ou cookie HttpOnly `periscope_token` emitido
no `/auth/login` e renovado em `/auth/refresh` (revogado por `jti` no `/auth/logout`). Documentação viva:

```bash
curl -s http://localhost:8080/openapi
# JSON: curl -s -H 'Accept: application/json' http://localhost:8080/openapi
```

Exemplo com cookie jar ou Bearer:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/periscope/rest/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' | jq -r .token)
curl -s http://localhost:8080/periscope/rest/auth/me -H "Authorization: Bearer $TOKEN"
```

Detalhes: [docs/modernization/fase-07-spa.md](docs/modernization/fase-07-spa.md).

## SPA React (`periscope-ui`)

```bash
cd periscope-ui
npm ci
npm run dev
# → http://localhost:5173/periscope/app/  (proxy para /periscope/rest no WildFly)
```

Embarcar manualmente no WAR (Maven **não** executa Node; `webapp/app/` é gitignored; no `Dockerfile` esse passo já é automático):

```bash
cd periscope-ui && npm run build:war
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B package
cp periscope-web/target/periscope.war /opt/jboss/wildfly-34.0.1.Final/standalone/deployments/
# → http://localhost:8080/periscope/app/
```

## Testes

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Unitários Maven (66 testes: 29 periscope-ejb + 37 periscope-web)
mvn -B clean verify

# Integração (Testcontainers MongoDB 7; skip automático sem Docker)
mvn -B verify -Pit

# Frontend: ESLint + Vitest/Testing Library + typecheck/build
cd periscope-ui && npm ci && npm run lint && npm test && npm run build

# E2E Playwright (6 specs com auto-seed; requer WildFly + Mongo + SPA em /periscope/app/)
npx playwright install --with-deps chromium
npm run test:e2e
# override: E2E_BASE_URL=http://localhost:5173/periscope/app/ npm run test:e2e
```

## CI / CD

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

| Job | Quando | O que faz |
|-----|--------|-----------|
| `build` | push/PR/`workflow_dispatch` | `mvn -B clean verify` |
| `integration` | push/PR/`workflow_dispatch` | `mvn -B verify -Pit` |
| `frontend` | push/PR/`workflow_dispatch` | `npm ci` + `npm run lint` + `npm test` + `npm run build` em `periscope-ui` |
| `dependency-check` | push/PR/`workflow_dispatch` | OWASP / NVD (`failBuildOnCVSS=7`, OSS Index desligado, `continue-on-error: true` devido a rate-limit do NVD sem chave) |
| `e2e` | push/PR/`workflow_dispatch` | Constrói `Dockerfile`, sobe `mongo:7` + WildFly 34, valida `/rest/health`, `/openapi` e `/periscope/app/` e executa Playwright E2E |

Workflow de homologação: [`.github/workflows/deploy-staging.yml`](.github/workflows/deploy-staging.yml) (disparo manual `workflow_dispatch` com confirmação `DEPLOY`, build/push de imagem, hook de deploy opcional, smoke health e Playwright E2E contra `vars.STAGING_BASE_URL`).

## Context root

`/periscope` (definido em `WEB-INF/jboss-web.xml`).

## Documentação

| Documento | Conteúdo |
|-----------|----------|
| [CHANGELOG.md](CHANGELOG.md) | Histórico de mudanças (Keep a Changelog) |
| [docs/modernization/README.md](docs/modernization/README.md) | Plano de modernização concluído |
| [docs/modernization/fase-07-spa.md](docs/modernization/fase-07-spa.md) | API REST + SPA |
| [docs/modernization/fase-08-qualidade-ci.md](docs/modernization/fase-08-qualidade-ci.md) | CI, testes, OpenAPI, E2E |
| [periscope-ui/README.md](periscope-ui/README.md) | Documentação do frontend React |
| [AGENTS.md](AGENTS.md) | Instruções para agentes / ambiente Cloud |
