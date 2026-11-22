# Periscope UI

SPA React (TypeScript) que constitui a interface principal e única do Periscope,
consumindo a API REST em `/periscope/rest`.

## Requisitos

- Node.js 22+ e npm 10+
- Backend Periscope rodando em `http://localhost:8080` (WildFly 34)

## Instalação

```bash
cd periscope-ui
npm ci
```

## Desenvolvimento

```bash
npm run dev
```

Abre em `http://localhost:5173/periscope/app/`. O Vite faz proxy de `/periscope/rest` →
`http://localhost:8080`, então não depende de CORS (CORS também está habilitado no backend
para `http://localhost:5173`).

Login padrão: `admin` / `123456`. Caso `mustChangePassword` esteja ativo no usuário, o modal
de alteração obrigatória de senha é exibido automaticamente após o login.

## Scripts

| Script | Descrição |
|--------|-----------|
| `npm run dev` | Servidor de desenvolvimento (porta 5173) |
| `npm test` | Testes unitários e de componentes (Vitest + Testing Library) |
| `npm run build` | Typecheck (`tsc -b`) + build em `dist/` |
| `npm run build:war` | Build direto em `../periscope-web/src/main/webapp/app/` |
| `npm run lint` | ESLint |
| `npm run format` | Prettier (write) |
| `npm run preview` | Preview do build local |
| `npm run test:e2e` | Playwright E2E (default `http://localhost:8080/periscope/app/`) |
| `npm run test:e2e:ui` | Playwright UI mode |

## Funcionalidades

- **Autenticação segura:** login via cookie `HttpOnly` (`credentials: 'include'`) + token em
  memória (zero escrita de JWT em `localStorage`), renovação em `/auth/refresh`, revogação em
  `/auth/logout` e modal de troca de senha (`/auth/change-password`).
- **Internacionalização (i18n):** alternância em tempo real entre português (`pt-BR`) e inglês
  (`en-US`), incluindo traduções das naturezas de depositantes e dos 13 relatórios.
- **Projetos:** criação/edição/exclusão com gestão de observadores (`observerIds`).
- **Patentes e Importação:** busca com filtros de completude/país/blacklisted, cadastro manual de
  patentes, edição completa de metadados e prioridades, upload/download GridFS (`presentation` e
  `patentInfo`) e importação multipart (Espacenet, Patentscope, DPMA).
- **Harmonização:** sugestões Fast-Join, navegação/filtro de entidades, criação, edição e reversão
  (undo) de regras com país, sigla de estado (`stateAcronym`) e natureza jurídica.
- **Relatórios:** todos os 13 relatórios com filtros completos (limite, tipo de data, intervalo,
  completude, nível IPC, tipo de classificação, tipo/nome de depositante, inventor), gráficos
  Recharts, descrições IPC, tabela paginada `docs-national-priority` e exportação CSV.
- **Usuários (`ADMIN`):** listagem, criação, edição e remoção (proteção contra auto-exclusão).

## Testes

### Unitários e de componentes (Vitest)

```bash
npm test
```

Executa `vitest run` sobre os testes em `src/__tests__/` (`client.test.ts`, `i18n.test.tsx`,
`UsersPage.test.tsx`, `ProjectsPage.test.tsx`, `PatentsPage.test.tsx`,
`HarmonizationPage.test.tsx`, `ReportsPage.test.tsx`).

### E2E (Playwright)

Requer backend WildFly + MongoDB e a SPA acessível (WAR ou `npm run dev`). As specs fazem
auto-seed de patente via `dpma-sample.csv` caso o banco esteja vazio.

```bash
npx playwright install --with-deps chromium
npm run test:e2e
# Dev server:
E2E_BASE_URL=http://localhost:5173/periscope/app/ npm run test:e2e
```

Specs em `e2e/`: `login`, `projects`, `patents`, `reports`, `harmonization` e `users`.

## Variáveis de ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `VITE_API_BASE` | `/periscope/rest` | Base da API REST (relativa ao host) |

## Embarcar no WAR

No Docker (`Dockerfile`), o estágio Node executa `npm run build` automaticamente e copia `dist/`
para o WAR. Para build manual local (Maven **não** executa Node):

```bash
cd periscope-ui
npm ci
npm run build:war

cd ..
mvn -B -pl periscope-web -am package
cp periscope-web/target/periscope.war /opt/jboss/wildfly-34.0.1.Final/standalone/deployments/
```

A pasta `periscope-web/src/main/webapp/app/` está no `.gitignore`. Rotas client-side
(`/periscope/app/login`, `/periscope/app/projects/...`, etc.) são atendidas pelo
`SpaFallbackFilter`, que devolve `/app/index.html` quando o asset não existe; `/periscope/`
redireciona automaticamente para `/periscope/app/`.

## Rotas da SPA

| Rota | Descrição |
|------|-----------|
| `/login` | Autenticação |
| `/projects` | CRUD de projetos e observadores |
| `/projects/:id/patents` | Lista paginada, filtros e cadastro manual de patentes |
| `/projects/:id/patents/:patentId` | Detalhe, edição completa e anexos GridFS |
| `/projects/:id/import` | Upload multipart de bases |
| `/projects/:id/harmonization` | Sugestões Fast-Join, entidades + regras (criar/editar/undo) |
| `/projects/:id/reports` | Todos os 13 relatórios + filtros + CSV (Recharts) |
| `/users` | CRUD de usuários (somente `ADMIN`) |
