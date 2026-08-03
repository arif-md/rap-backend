# backend (rap-backend)

> Shared conventions (architecture, inter-service comms, coding standards, git/submodule
> workflow) live in the parent aggregator's CLAUDE.md. This file covers only what is
> specific to this service.

## Purpose
Spring Boot REST API for the RAP application: owns user auth (dual OIDC/Azure AD → JWT),
applications (including "admissions" - applications whose latest workflow status is
ACCEPTED), universities, and a local workflow "tasks" table. It does **not**
own BPMN process definitions or human-task orchestration logic — that's `rap-processes`
(jBPM, port 8090). Despite the name, `WorkflowController`/`ProcessService`/`ProcessHandler`
still read/write a local `tasks` SQL table (migration `V6__Create_task_table.sql`) and are
independent of jBPM. A separate, growing integration point calls the jBPM process service
directly via the KIE Server Java client (`KieClient`/`BaseController.startProcess()` —
see Dependencies & integrations below); more jBPM calls (retrieving active process
instances, retrieving tasks) are planned there.

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
- `/api/applications` — CRUD + `/code/{code}`, `/status/{status}`, `/search`, `/count`, `/my`,
  `/my/admissions`, `/university/{id}`, `/university/{id}/admissions` (the `/admissions`
  variants are ACCEPTED-only, backing the dashboard's "Admissions" tabs - see
  `ApplicationService.getAcceptedApplicationsBy*`)
- `/api/applications/submissions` — public application intake (`ApplicationSubmissionHandler`)
- `/api/workflow/tasks` — paginated tasks for current user, `/tasks/{id}` (see Purpose note above)
- `/api/admin/**` — `@PreAuthorize("hasRole('ADMIN')")` at the class level; user/role management
- `/api/universities`, `/api/config/environmentProperties` (frontend runtime config)
- `/actuator/*` — `health,info,flyway` exposed (`management.endpoints.web.exposure.include`)

## Data model
- Schema: `RAP` (see `docs/DATABASE-SCHEMA-STRATEGY.md` for why it's isolated from jBPM's `JBPM` schema)
- Migrations: `src/main/resources/db/migration/` — `V1` initial schema, `V2` auth tables,
  `V3` applications, `V4` permits (dropped by `V11` - see below), `V5` seed data, `V6` tasks,
  `V7` jBPM integration tables, `V8` ref-table seed data, `V9` application internal review
  (reviewer signature + review date), `V10` attachments, `V11` drops `RAP.permit`. Add new
  ones as `V12__*.sql`.
- `V4__Create_permit_tables.sql` created a standalone `RAP.permit` table/CRUD stack that was
  never wired to the frontend - the "My Permits"/"Permits" dashboard tabs were always backed
  by `RAP.application` filtered to ACCEPTED status. That table and its `Permit`/`PermitService`/
  `PermitHandler`/`PermitMapper` stack were removed (`V11`) when the "permit" concept was
  renamed to "admission" throughout the app; don't reintroduce a separate permit entity for
  the admissions tabs, they belong on `ApplicationService.getAcceptedApplicationsBy*`.
- Mapper XML ↔ mapper interface pairs live in `src/main/resources/mapper/*.xml` and
  `repository/mapper/*.java` — one pair per table (`ApplicationMapper`, `UniversityMapper`,
  `UserMapper`, `UserRoleMapper`, `RoleMapper`, `RefreshTokenMapper`, `RevokedTokenMapper`,
  `ProcessMapper` (tasks)).
- UUID primary keys go through a custom `UuidTypeHandler` (`config/UuidTypeHandler.java`).

## Dependencies & integrations
- Downstream: `rap-frontend` (Angular, calls this API directly)
- Peer: `rap-processes` (jBPM, port 8090) — called via the KIE Server Java client
  (`org.kie.server.client`, `kie-server-client` dependency), not a hand-rolled REST client.
  `KieClient` (`utils/KieClient.java`) lazily builds a singleton `KieServicesClient` from
  `env.kieServerURL`/`env.kieServerUser`/`env.kieServerPwd` (set at the bottom of
  `application.properties`) and hands out typed sub-clients (`ProcessServicesClient`,
  `UserTaskServicesClient`, `QueryServicesClient`, `DocumentServicesClient`, plus admin
  clients). `BaseController.startProcess()` is the current entry point, used by
  `ApplicationSubmissionController` to start a process instance on application submission.
  Expect more jBPM calls here going forward (querying active process instances, retrieving
  user tasks) via `KieClient.getProcessServicesClient()` / `getUserTaskServicesClient()` /
  `getQueryServicesClient()`. This is separate from the local `tasks` table read by
  `WorkflowController` (see Purpose) — don't conflate the two.
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
  original CRUD handlers (`ApplicationHandler`, `UserHandler`, etc.) live
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
- `ProcessService`/`WorkflowController` (local `tasks` table) and `KieClient`/
  `BaseController.startProcess()` (real jBPM calls) are two separate, non-overlapping
  integration points that happen to sound alike — don't conflate them when adding a
  jBPM-backed feature.
- `ApplicationAttributes` (`config/ApplicationAttributes.java`) pushes the `env.kieServer*`
  properties into `KieClient`'s static fields via `afterPropertiesSet()`. It **must** stay
  a default (singleton-scoped) bean — giving it a non-singleton scope (e.g.
  `@Scope("application")`) means nothing ever triggers Spring to instantiate it (custom
  scopes aren't eagerly created at context startup, and nothing else injects this bean), so
  `afterPropertiesSet()` silently never runs and `KieClient`'s URL/user/password stay
  `null`. There's no startup error — it only surfaces later as `KieServicesClientImpl`
  throwing `NoEndpointFoundException: No available endpoints found` the first time a jBPM
  call is made.
- `docs/DATABASE-SCHEMA-STRATEGY.md` references a `V13__Create_RAP_schema.sql` migration
  that does not exist in this repo (migrations currently stop at `V6`) — treat that doc as
  partially aspirational/stale, not authoritative, when reasoning about migration numbering.
- Auth endpoints (`/auth/user`, `/auth/check`, etc.) are deliberately `permitAll()` in
  `SecurityConfig` even though they require a valid session — they do their own JWT-cookie
  validation and return JSON 401s. Don't "fix" this by moving them under `authenticated()`;
  that breaks the frontend's ability to distinguish "not logged in" from a redirect-driven CORS error.
- Local dev DB auth is SQL auth (`sa`/password); Azure uses passwordless Managed Identity
  (`authentication=ActiveDirectoryMSI`) — don't hardcode one path when touching `spring.datasource.*`.
