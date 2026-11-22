# AGENTS.md

## Cursor Cloud specific instructions

Periscope is a Jakarta EE 10 patent-analysis web app (UFMT). It is a Maven
multi-module project: `periscope-ejb` (EJB jar, bundled into the war) and
`periscope-web` (war), plus the React/Vite/TypeScript SPA in `periscope-ui`
embedded into the WAR at `periscope-web/src/main/webapp/app/`. There is **no EAR** —
packaging is a single WAR (`periscope-web/target/periscope.war`). Persistence is
**MongoDB** via Morphia 2 (no JPA/SQL); harmonization uses **Lucene 9**; the **React
SPA** (`periscope-ui`, with `pt-BR`/`en-US` i18n) is the sole UI (JSF/PrimeFaces
and `tools/legacy-m2/` have been completely removed).

### Toolchain / runtime
- **Build & run on JDK 21** (`maven.compiler.release=21`). Export `JAVA_HOME`
  pointing to a JDK 21 installation before every `mvn` invocation.
- **Maven 3.9+** (`mvn`) and **Node.js 22+ / npm 10+** (for `periscope-ui`).

### App server: WildFly 34.0.1.Final
- Recommended server: `/opt/jboss/wildfly-34.0.1.Final` (JDK 21) or container via
  `Dockerfile` (`quay.io/wildfly/wildfly:34.0.1.Final-jdk21`).
- Always ensure `JBOSS_HOME` points to WildFly 34 (do not boot older WildFly 8/AS 7 copies).
- Start:
  `unset JBOSS_HOME; export JBOSS_HOME=/opt/jboss/wildfly-34.0.1.Final JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64; cd "$JBOSS_HOME" && ./bin/standalone.sh -b 0.0.0.0`
- Deploy: copy `periscope-web/target/periscope.war` to
  `/opt/jboss/wildfly-34.0.1.Final/standalone/deployments/`
  (marker `periscope.war.deployed` on success; `.failed` on error).
- Context root `/periscope` via `WEB-INF/jboss-web.xml`; `/periscope/` redirects to
  `/periscope/app/`.

### MongoDB (driver sync 5.x / Morphia 2)
- **MongoDB:** Morphia 2.5.3 + `mongodb-driver-sync` 5.2.1. Connection via
  `MONGODB_URI` (default `mongodb://localhost:27017`) and `MONGODB_DATABASE`
  (default `Periscope`).
  Morphia **2.5.x** is required with driver 5.x (`MapCodec` is final in bson 5;
  Morphia 2.4.x cannot link). Jackson in the WAR is pinned to **2.17.0** (same
  line as WildFly 34) and server Jackson modules are excluded in
  `WEB-INF/jboss-deployment-structure.xml` so the seed YAML loader does not mix
  classloaders.
- Server: **MongoDB 4.4+** works for local smoke; Docker Compose (`docker-compose.yml`
  and `docker-compose.prod.yml`) targets **mongo:7**.

### Docker
- The 3-stage `Dockerfile` builds `periscope-ui` with Node 22, copies `dist/` into
  `periscope-web/src/main/webapp/app`, builds the WAR with Maven + JDK 21, and deploys
  to WildFly 34 with MicroProfile OpenAPI enabled and a `HEALTHCHECK` on `/periscope/rest/health`.
- Production compose (`docker-compose.prod.yml` + `deploy/Caddyfile` + `.env.example`)
  configures Mongo auth on an internal network, Caddy HTTPS termination,
  `PERISCOPE_PRODUCTION=true`, mandatory `PERISCOPE_JWT_SECRET`, and initial admin
  seed overrides.
- If the local VM lacks Docker daemon access, verify Docker/Compose/CI files by schema
  validation & code review and validate WAR packaging natively with `npm run build:war`
  + `mvn -B clean verify`.

### Build, deploy, run
- Embed SPA + build WAR:
  ```bash
  cd periscope-ui && npm ci && npm run build:war && cd ..
  mvn -B clean package
  ```
  → `periscope-web/target/periscope.war` (contains `app/index.html`, zero `.xhtml`/PrimeFaces).
- The app writes its Lucene index to **`/opt/periscope`**, which must exist and be
  writable by the server process. Override with env `PERISCOPE_DIR`.
- App URL: `http://localhost:8080/periscope/app/` (`/periscope/` redirects to `app/`).
- Default dev login: **admin / 123456** (seeded into MongoDB on first deploy by `SeedBean`;
  configurable via `PERISCOPE_ADMIN_USERNAME`, `PERISCOPE_ADMIN_PASSWORD`,
  `PERISCOPE_ADMIN_EMAIL`, `PERISCOPE_FORCE_ADMIN_PASSWORD_CHANGE`).

### Tests / lint
- Backend unit tests: `mvn -B clean verify` (66 tests: 29 in `periscope-ejb` + 37 in
  `periscope-web`). Do not require Mongo or Docker.
- Integration tests (Testcontainers MongoDB 7): `mvn -B verify -Pit`. Require Docker;
  gracefully skipped when Docker is unavailable (`@Testcontainers(disabledWithoutDocker = true)`).
- Frontend (`periscope-ui`): `npm ci && npm run lint && npm test && npm run build`
  (ESLint + Vitest / Testing Library — 7 test files / 10 tests + TypeScript/Vite build).
- E2E Playwright: `cd periscope-ui && npm run test:e2e` (default
  `E2E_BASE_URL=http://localhost:8080/periscope/app/`). Install browser:
  `npx playwright install --with-deps chromium` (omit `--with-deps` if apt/sudo fails).
  Specs (with self-seeding on empty DB): `e2e/login|projects|patents|reports|harmonization|users.spec.ts`.
- OpenAPI: subsystem WildFly `microprofile-openapi-smallrye` —
  `./tools/enable-wildfly-openapi.sh` (embedded CLI, idempotent; automated in `Dockerfile`).
  Spec available at `GET http://localhost:8080/openapi`.
- CI / CD: `.github/workflows/ci.yml` (`build`, `integration`, `frontend` with Vitest,
  `dependency-check` with `failBuildOnCVSS=7`, and automated Docker `e2e` job on push/PR/dispatch).
  Staging deploy workflow: `.github/workflows/deploy-staging.yml` (`workflow_dispatch`
  with `confirm=DEPLOY`).
- Coverage: JaCoCo 0.8.12 on `test` lifecycle (`*/target/site/jacoco/`).
- Health: `GET http://localhost:8080/periscope/rest/health` → JSON
  `status` / `mongodb` / `luceneIndex` (200 if UP, 503 if DOWN).
- REST API base `/periscope/rest`:
  - Auth: `POST /auth/login` (sets HttpOnly `periscope_token` cookie + returns JWT),
    `GET /auth/me`, `POST /auth/refresh`, `POST /auth/change-password`, `POST /auth/logout`
    (revokes `jti` + clears cookie). Requests use `Authorization: Bearer <token>` or the
    `periscope_token` HttpOnly cookie.
  - Key env vars: `PERISCOPE_PRODUCTION`, `PERISCOPE_COOKIE_SECURE`, `PERISCOPE_JWT_SECRET`,
    `PERISCOPE_CORS_ORIGINS`, `PERISCOPE_JWT_EXPIRATION_HOURS`, `PERISCOPE_ADMIN_USERNAME`,
    `PERISCOPE_ADMIN_PASSWORD`, `PERISCOPE_ADMIN_EMAIL`, `PERISCOPE_FORCE_ADMIN_PASSWORD_CHANGE`.
- SPA: `http://localhost:8080/periscope/app/` (or `npm run dev` on port 5173).
- No backend Spotless linter; ESLint/Prettier in `periscope-ui`.
