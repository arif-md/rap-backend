# backend (rap-backend)

> Shared conventions (architecture, inter-service comms, coding standards, git/submodule
> workflow) live in the parent aggregator's CLAUDE.md. This file covers only what is
> specific to this service.

## Purpose
Spring Boot REST API for the RAP application: owns user auth (dual OIDC/Azure AD → JWT),
applications, permits, universities, and a local workflow "tasks" table. It does **not**
own BPMN process definitions or human-task orchestration logic — that's `rap-processes`
(jBPM, port 8090). Despite the name, `WorkflowController`/`ProcessService`/`ProcessHandler`
currently read/write a local `tasks` SQL table (migration `V6__Create_task_table.sql`) —
there is no REST/HTTP call from this codebase into the jBPM service yet.

## Tech stack
- Spring Boot 3.5.5, Java 17, packaged as `.war` (Tomcat provided, embeddable)
- MyBatis 3.0.5 (NOT JPA/Hibernate — no `@Entity` anywhere, mappers are hand-written XML)
- Flyway migrations against SQL Server, isolated into a dedicated `RAP` schema (jBPM owns
  a separate `JBPM` schema in the same database — see `docs/DATABASE-SCHEMA-STRATEGY.md`)
- Spring Security with two parallel OAuth2/OIDC login flows + a custom JWT layer on top
  (see Authentication below) — this is more involved than a typical Spring Security setup

## Commands
Run from `backend/` (see parent CLAUDE.md for the full `dev.ps1`/`make` command table):
- Build:          `.\mvnw.cmd clean package -DskipTests` (or `.\dev.ps1 Build`)
- Test (all):     `.\mvnw.cmd test` (or `.\dev.ps1 Test`)
- Test (single):  `.\mvnw.cmd test -Dtest=ClassName#methodName`
- Run locally (hot reload): `.\run-local.ps1` (requires `.\dev.ps1 Dev-Start` or `Dev-Full` first for DB/Keycloak) — prefer the VS Code task "Run Spring Boot Dev Server" per parent CLAUDE.md
- Fix a Flyway checksum mismatch after editing an applied migration locally: `.\dev.ps1 Flyway-Repair`
- Connect to the local DB: `.\dev.ps1 DB-Connect`

## API surface
- Base path: `/api` (port 8080 locally); auth endpoints live at `/auth` (not `/api/auth`)
- Public/no-auth: `/api/public/**`, `/api/config/**`, `/actuator/health`, `/error`, `/auth/**`
  (auth endpoints self-handle authentication via the JWT cookie so Spring Security doesn't
  302-redirect XHR calls into a CORS failure — see `SecurityConfig`)
- `/auth` — `login`, `sso-login`, `refresh`, `logout`, `user`, `check`
- `/api/applications` — CRUD + `/code/{code}`, `/status/{status}`, `/search`, `/count`, `/my`, `/university/{id}`
- `/api/applications/submissions` — public application intake (`ApplicationSubmissionHandler`)
- `/api/permits` — `/my`, `/{id}`, `/number/{permitNumber}`, `/university/{universityId}`
- `/api/workflow/tasks` — paginated tasks for current user, `/tasks/{id}` (see Purpose note above)
- `/api/admin/**` — `@PreAuthorize("hasRole('ADMIN')")` at the class level; user/role management
- `/api/universities`, `/api/config/environmentProperties` (frontend runtime config)
- `/actuator/*` — `health,info,flyway` exposed (`management.endpoints.web.exposure.include`)

## Data model
- Schema: `RAP` (see `docs/DATABASE-SCHEMA-STRATEGY.md` for why it's isolated from jBPM's `JBPM` schema)
- Migrations: `src/main/resources/db/migration/` — `V1` initial schema, `V2` auth tables,
  `V3` applications, `V4` permits, `V5` seed data, `V6` tasks. Add new ones as `V7__*.sql`.
- Mapper XML ↔ mapper interface pairs live in `src/main/resources/mapper/*.xml` and
  `repository/mapper/*.java` — one pair per table (`ApplicationMapper`, `PermitMapper`,
  `UniversityMapper`, `UserMapper`, `UserRoleMapper`, `RoleMapper`, `RefreshTokenMapper`,
  `RevokedTokenMapper`, `ProcessMapper` (tasks)).
- UUID primary keys go through a custom `UuidTypeHandler` (`config/UuidTypeHandler.java`).

## Dependencies & integrations
- Downstream: `rap-frontend` (Angular, calls this API directly)
- Peer: `rap-processes` (jBPM) — not yet called over HTTP from this service despite the
  `Workflow`/`Process` naming (see Purpose)
- External: local Keycloak (OIDC, port 9090) or Azure Entra ID (Azure AD SSO) for login;
  Azure SQL / SQL Server 2022 container for data
- Key env vars (see `.env.example` for the full list): `JWT_SECRET`,
  `JWT_ACCESS_TOKEN_EXPIRATION_MINUTES`, `JWT_REFRESH_TOKEN_EXPIRATION_DAYS`,
  `OIDC_CLIENT_ID`/`OIDC_CLIENT_SECRET`, `AZURE_AD_TENANT_ID`/`AZURE_AD_CLIENT_ID`/`AZURE_AD_CLIENT_SECRET`,
  `AZURE_SQL_CONNECTIONSTRING`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL`

## Project structure (non-obvious parts)
- Package root is `x.y.z.backend` — a placeholder groupId/package that was never renamed;
  don't "fix" it as a drive-by change, it's load-bearing across every file.
- `domain/model` — MyBatis type-alias target (plain POJOs, no JPA annotations)
- `domain/handler` vs `handler` — two handler packages exist; newer feature-specific
  handlers (e.g. `ApplicationSubmissionHandler`) live under `domain/handler`, while the
  original CRUD handlers (`ApplicationHandler`, `PermitHandler`, `UserHandler`, etc.) live
  directly under `handler`. Follow whichever sibling pattern matches the feature you're touching.
- `config/CurrentUser` + `config/UserArgumentResolver` — lets controllers take a
  `CurrentUser user` parameter directly (resolved from the Spring Security principal set by
  `JwtAuthenticationFilter`) instead of pulling it from `SecurityContextHolder` manually.
- Profile-specific `application-{local,docker,prod,test,train}.properties` and matching
  `bootstrap-*.properties` (the bootstrap files exist only because Azure App Configuration's
  Spring Cloud 5.x integration still uses the legacy bootstrap-context mechanism, not
  `spring.config.import`).

## Conventions specific to this service
- Strict layering: `@RestController` → `@Service` (`@Transactional`, business rules) →
  `@Component` Handler (data access only) → MyBatis Mapper interface + XML. Controllers
  must not contain business logic or direct mapper calls; Handlers must not contain
  `@Transactional` (that's the Service's job).
- Auth is dual-provider: an external OIDC provider (Keycloak locally, e.g. Login.gov-style
  in prod) for external users, and Azure Entra ID for internal staff SSO, unified through
  `DelegatingOidcUserService`. Both flows land in `OAuth2AuthenticationSuccessHandler`,
  which mints this app's own JWT (access + refresh) stored in httpOnly cookies — the OIDC
  provider tokens themselves are not used for subsequent API calls.
- CORS config is intentionally a `CorsConfigurationSource` lambda reading from `Environment`
  on every request (not a static bean) so Azure App Configuration refresh can change
  `cors.allowed-origins` without a restart — see the comment in `SecurityConfig.corsConfigurationSource()`.
- Role checks use method security (`@PreAuthorize("hasRole('ROLE')")`) at the controller
  (or class) level, not manual role checks in handlers/services.

## Gotchas / do & don't
- Don't add a live HTTP client call from this service into the jBPM process service without
  checking with the user first — the current `ProcessService`/`WorkflowController` naming
  looks like it should proxy jBPM but is actually a self-contained local table; conflating
  the two is an easy mistake.
- `docs/DATABASE-SCHEMA-STRATEGY.md` references a `V13__Create_RAP_schema.sql` migration
  that does not exist in this repo (migrations currently stop at `V6`) — treat that doc as
  partially aspirational/stale, not authoritative, when reasoning about migration numbering.
- Auth endpoints (`/auth/user`, `/auth/check`, etc.) are deliberately `permitAll()` in
  `SecurityConfig` even though they require a valid session — they do their own JWT-cookie
  validation and return JSON 401s. Don't "fix" this by moving them under `authenticated()`;
  that breaks the frontend's ability to distinguish "not logged in" from a redirect-driven CORS error.
- Local dev DB auth is SQL auth (`sa`/password); Azure uses passwordless Managed Identity
  (`authentication=ActiveDirectoryMSI`) — don't hardcode one path when touching `spring.datasource.*`.
