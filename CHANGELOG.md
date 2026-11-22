# Changelog

Todas as mudanças notáveis deste projeto são documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Modernização **100% concluída**: backend Jakarta EE 10 / WildFly 34, paridade funcional
completa da API REST e SPA React, empacotamento Docker multi-stage com SPA embarcada,
descomissionamento integral do legado JSF/PrimeFaces, endurecimento de segurança para produção
e suíte completa de testes e CI/CD.

### Added

- **Paridade completa da API REST e SPA (`periscope-ui`):**
  - Todos os **13 relatórios** do sistema com filtros (`limit`, `dateType`, `startDate`,
    `endDate`, `complete`, `ipcLevel`, `classificationType`, `applicantType`,
    `applicantName`, `inventorName`), descrições de seções IPC, exportação CSV (`Content-Disposition`)
    e tabela paginada para documentos com prioridade nacional (`docs-national-priority`), além
    de autocomplete `/reports/applicants` e `/reports/inventors`.
  - Endpoints de referência `/reference/countries`, `/reference/applicant-types` e
    `/reference/users`.
  - Gestão de **observadores de projeto** (`observerIds`, `POST/DELETE /projects/{id}/observers/{userId}`).
  - Criação manual de patentes (`POST /projects/{id}/patents`), edição completa de metadados e
    prioridades (`PUT /patents/{id}`), filtros por país/blacklisted e upload/download GridFS na UI
    (`presentation` e `patentInfo`).
  - Harmonização completa: listagem/filtro de entidades (`GET /harmonization/projects/{id}/entities`),
    edição e reversão (undo) de regras (`PUT /rules/{ruleId}`, `POST /rules/{ruleId}/undo`) com
    país, sigla de estado (`stateAcronym`) e natureza do depositante.
  - CRUD completo de usuários (`ADMIN`) na SPA (`UsersPage`).
  - Internacionalização (**i18n**) completa na SPA entre `pt-BR` e `en-US`, cobrindo rótulos de
    natureza jurídica de depositantes e todos os relatórios.
- **Segurança e operação em produção:**
  - Endpoints `POST /auth/refresh` (rotação/revogação de JWT), `POST /auth/change-password` e
    suporte ao flag `mustChangePassword` no modelo `User` com modal obrigatório na SPA.
  - Seed administrativo inicial configurável via `PERISCOPE_ADMIN_USERNAME`,
    `PERISCOPE_ADMIN_PASSWORD`, `PERISCOPE_ADMIN_EMAIL` e `PERISCOPE_FORCE_ADMIN_PASSWORD_CHANGE`.
  - Revogação de JWT por `jti` via `TokenRevocationStore` (memória + coleção Mongo TTL) em
    `/auth/logout` e `/auth/refresh`.
  - Stack de produção [`docker-compose.prod.yml`](docker-compose.prod.yml) com MongoDB 7
    autenticado, rede interna isolada, `deploy/Caddyfile` (HTTPS/headers de segurança) e
    [`.env.example`](.env.example).
- **Empacotamento e CI/CD:**
  - `Dockerfile` multi-stage de 3 estágios (`node:22-alpine` → `maven:3.9-eclipse-temurin-21`
    copiando `/ui/dist` para `webapp/app/` → `quay.io/wildfly/wildfly:34.0.1.Final-jdk21`) com
    `HEALTHCHECK` e OpenAPI habilitado.
  - Redirecionamento automático de `/periscope/` para `/periscope/app/` via `index.html`.
  - Suíte frontend **Vitest + Testing Library** (`periscope-ui/src/__tests__/`: 7 arquivos, 10 testes).
  - Novo spec E2E `periscope-ui/e2e/users.spec.ts` e auto-seed de patentes via `dpma-sample.csv`
    nos specs E2E em banco vazio.
  - Job `e2e` real no `.github/workflows/ci.yml` com Docker (`mongo:7` + container da aplicação)
    e Playwright E2E executado em push/PR/`workflow_dispatch`.
- **Base da modernização (PRs #12–#16):**
  - Empacotamento **WAR único** (`periscope-web/target/periscope.war`), context-root `/periscope`,
    `docker-compose.yml`, scripts `scripts/dev-up.sh` / `scripts/dev-down.sh`.
  - Suíte unitária backend JUnit 5 + Mockito + AssertJ (expandida para 66 testes) e ITs
    Testcontainers (`PatentRepositoryIT`, `ReferenceCycleIT`) no perfil Maven `it`.
  - Health check `GET /periscope/rest/health` e OpenAPI 3 em `/openapi`.

### Changed

- **SPA React é a única interface:** `periscope-ui` passou a atender 100% das funcionalidades e
  `/periscope/` redireciona para `/periscope/app/`.
- **Autenticação da SPA e REST:** token JWT deixou de ser gravado em `localStorage`; login e
  refresh emitem cookie `HttpOnly` `SameSite=Lax` (`periscope_token`), aceito pelo `JwtAuthFilter`
  em conjunto com `Authorization: Bearer`.
- **JWT Service:** parsing e assinatura/validação de payload agora utilizam Jackson ObjectMapper
  com verificação estrita de `alg == HS256`; modo `PERISCOPE_PRODUCTION=true` falha na inicialização
  caso `PERISCOPE_JWT_SECRET` esteja ausente ou tenha menos de 32 bytes.
- **`MainNatureApplicantReport`, `PatentValidator` e repositórios:** desacoplamento total de
  `FacesContext`/`@Inject` em estado mutável de instância; filtros de relatório por status
  `completed` e intervalo de datas aplicados consistentemente em todas as agregações;
  `PatentResource.update` preserva override explícito de `completed`.
- **OWASP dependency-check e deploy de staging:** `failBuildOnCVSS` ajustado de 11 para `7`;
  `.github/workflows/deploy-staging.yml` ativado via `workflow_dispatch` (`confirm=DEPLOY`).
- Baseline de runtime: servidor alvo atualizado de WildFly 8 / EAR para **WildFly 34.0.1.Final**
  (WAR único), **Morphia 2.5.3**, **mongodb-driver-sync 5.2.1** e Jackson **2.17.0**.

### Fixed

- **IDOR em `GET /rest/files/{id}`:** `FileResource` passa a resolver o `projectId` dono do arquivo
  GridFS e validar permissão via `requireProject`.
- **Agregações de depositantes por natureza (`NatureApplicantRepository` e `ApplicantRepository`):**
  uso de `$ifNull` / `$or` entre `applicants.type.name` e `applicants.nature.name`, escape de regex
  com `Pattern.quote`, reset do contador total e proteção contra datas nulas/listagens nulas.
- **`ApplicantTypeRepository`:** corrigida guarda de contagem (`count > 0`) e projeção/conversão de
  `PatentDTO`, `RuleDTO` e payloads JSON desconhecidos.
- Ciclo de `@Reference(lazy = true)` Morphia (`Project.patents` ↔ `Patent.project`) e paginação
  de patentes sem esgotamento de pool.

### Security

- Controle de acesso por projeto em `GET /rest/files/{id}` e na edição/gestão de observadores de projetos.
- Hash de senhas com **PBKDF2-HMAC-SHA256** (>= 120.000 iterações, salt aleatório) com migração
  transparente e política de senha mínima (8 caracteres) em `/auth/change-password`.
- Cookie `HttpOnly` + `SameSite=Lax` (+ `Secure` em HTTPS/`PERISCOPE_COOKIE_SECURE=true`),
  revogação ativa de JWT via `jti` no logout/refresh, fail-fast de segredo JWT em produção e
  sanitização de `Content-Disposition`.

### Removed

- **Camada legada JSF / PrimeFaces e resíduos:**
  - Pacotes JSF Java (`controller/`, `converter/`, `lazy/`, `managedbean/`, `qualifier/`, `util/`,
    `UserAccessFilter`, `br.ufmt.periscope.main.*`), páginas `.xhtml`, templates, imagens/CSS/JS
    legados, `messages_*.properties`, `WEB-INF/faces-config.xml` e `WEB-INF/taglib.xml`.
  - Dependências `primefaces` e `org.primefaces.themes:bootstrap`, módulo `periscope-ear`,
    repositório local `tools/legacy-m2/` e `tools/install-legacy-artifacts.sh`.
  - Arquivos obsoletos da raiz e recursos antigos: `Capfile`, `config/deploy.rb`, `META-INF/faces-config.xml`,
    `descriptors`, `tmp-http-before.tsv`, descritores Eclipse (`.project`, `.classpath`, `.settings`),
    `import.sql`, `META-INF/jboss.xml`, `js/*.js` e `login.feature` órfão.

## [1.0.0] — 2026-07-24

Baseline pós-Fases 0–5 (parcial), mergeada via PR #11 e PRs anteriores (#1, #4, #5, #10).

### Added

- Compilação e runtime em **Java 21** / **Jakarta EE 10**.
- Persistência com **Morphia 2** + **mongodb-driver-sync 5.2.1** (sem JPA/SQL).
- Harmonização Fast-Join em **Apache Lucene 9.12.0**.
- Extração de PDF com **PDFBox 3.0.3** e importadores com **Apache POI 5.3.0**.
- Ambiente local documentado e seed inicial (`admin` / `123456`) via `SeedBean`.

### Changed

- Migração completa `javax.*` → `jakarta.*` e beans gerenciados para CDI.
- Build Maven moderno (`maven.compiler.release=21`), sem repositórios `http://` bloqueados.

[Unreleased]: https://github.com/brunomartinsmv/periscope/compare/95a500c...HEAD
[1.0.0]: https://github.com/brunomartinsmv/periscope/tree/95a500c
