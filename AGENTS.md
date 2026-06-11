# AGENTS.md — Spring Boot 4 Backend Base

## Stack

- **Spring Boot 4.0.5 + Java 25** — Maven single-module project. `Application.java` uses Java 25 implicit class syntax: `void main(String[] args)` (no `public static`).
- **MyBatisPlus 3.5.16** — ORM with logical delete (`deleted` field, 1=deleted, 0=active)
- **MyBatisPlus-Join 1.5.6** — For complex join queries (use `MPJBaseMapper` instead of `BaseMapper`)
- **Spring Security + JWT** — Stateless auth; refresh token via HttpOnly cookie
- **MySQL + Redis** — Required for dev
- **MapStruct 1.6.3** — DTO mapping (not to be confused with MyBatisPlus mappers)
- **OpenHTMLToPDF** — PDF generation from Thymeleaf templates

## Local Development

### Prerequisites

1. Copy `.env.example` → `.env.local` (gitignored) and fill values
2. `JWT_SECRET` must be ≥ 32 characters
3. MySQL and Redis must be running

### Run

- **VSCode**: Use bundled `.vscode/launch.json` — picks up `.env.local` automatically via `envFile`
- **CLI**: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- **Debug**: `Debug Application (JDWP 5005)` launch config exposes port 5005

### Build

```bash
mvn clean package -DskipTests
```

Active profile defaults to `dev` via `<active.profile>` property in `pom.xml`.

### Validation Order

When making changes, run in this order:
```bash
mvn fmt:format && mvn compile
```

## Code Style

- **Google Java Format** enforced by `fmt-maven-plugin` (GOOGLE style)
- Pre-commit hook (`.githooks/pre-commit`) auto-runs `mvn fmt:format` and re-stages changed `.java` files
- Hooks path is auto-configured via `maven-antrun-plugin` on `mvn validate`
- **Always run `mvn fmt:format` before committing** if the hook isn't active

## Architecture

```
com.lincsoft/
├── Application.java           # Entry point (@EnableScheduling). Java 25 implicit class.
├── config/                    # Spring configs, security, MyBatisPlus, Redis, Async
├── controller/                # REST controllers, grouped by module (auth, master, log, ...)
│   └── */vo/                  # Request/response DTOs per controller
├── services/                  # Business logic, grouped by module (master, system, report, ...)
├── entity/                    # MyBatisPlus entities (master/*, system/*)
├── mapper/                    # MyBatisPlus data access mappers (NOT MapStruct)
├── mapstruct/                 # MapStruct DTO converters (NOT MyBatisPlus)
├── aspect/                    # Operation logging aspect
├── filter/                    # Rate limiting, CSRF, JWT filters
├── interceptor/               # Access logging
├── common/                    # Result<T>, PageRequest, BaseEntity, SelectOption
├── constant/                  # Enums (Module, SubModule, MessageEnums, RoleCodeEnums)
├── util/                      # JwtUtil, LogUtil, IpChecker
└── event/                     # Spring events (e.g., UserCreatedEvent)
```

### API Response Format

All responses are wrapped in `Result<T>` with fields: `code`, `messageKey`, `messageArgs`, `message`, `data`.
- `messageKey` is an i18n key resolved at serialization time by `GlobalResponseAdvice`
- Use `@IgnoreResultWrapper` to bypass wrapping for specific endpoints

### Security Endpoints

Public endpoints (no auth, no CSRF) and auth-only whitelist are configured in `application.yml` under `app.security.public-endpoints` and `app.security.auth-only-whitelist`. The dev profile adds Swagger/OpenAPI paths.

## Database

- **DDL**: `src/main/resources/db/system.sql` (non-partitioned), `db/master.sql`, `db/oa.sql`
- **Partitioned logs** (optional): `db/partition/*.sql` for `sys_access_log`, `sys_error_log`, `sys_operation_log`
- Logical delete is globally configured in `application.yml`: field `deleted`, values `1`/`0`

## Important Constraints

### Reverse Proxy — X-Forwarded-For

The app uses `X-Forwarded-For` for rate limiting (`RateLimitFilter`) and access logs (`AccessLogInterceptor`). **The reverse proxy must overwrite this header with the real client IP**, not append to it. In Nginx:

```nginx
proxy_set_header X-Forwarded-For $remote_addr;  # NOT $proxy_add_x_forwarded_for
```

### Management Port Isolation

Actuator runs on **port 9090** (business port is 8080). This port **must never be exposed to the public internet** — firewall it to Prometheus IPs only.

### Test Coverage

Only one test exists (`PasswordEncoderTest`). There is no test framework beyond `spring-boot-starter-test`. Do not add tests unless explicitly asked.

To generate a BCrypt hash for a password:
```bash
mvn -Dtest=PasswordEncoderTest#generateHashForCustomPassword -Dpassword=yourPassword test
```

### File Uploads

Uploaded files are stored in `./uploads/` (configurable via `UPLOAD_DIR` env var). This directory is gitignored.

## OpenCode Config

This repo's `opencode.json` grants access to the paired frontend project at `D:/Projects/github/vue3-vuetify4-frontend-base/`. When working on cross-repo features, respect both `AGENTS.md` files.
