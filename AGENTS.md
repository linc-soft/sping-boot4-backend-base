# AGENTS.md — Spring Boot 4 Backend Base

## Stack

- **Spring Boot 4.0.5 / Java 25** — main class `com.lincsoft.Application`
- **Maven** — profiles: `dev` (default), `test`, `prod` (`-P` flag or `SPRING_PROFILES_ACTIVE`)
- **ORM**: MyBatisPlus 3.5.16 with MySQL
- **Auth**: Spring Security + JWT (JJWT 0.13.0), stateless
- **Cache**: Redis (Spring Data Redis + Lettuce)
- **Rate limiting**: Bucket4j (token-bucket, disabled in dev)
- **API docs**: SpringDoc OpenAPI + Knife4j UI
- **PDF**: Thymeleaf + OpenHTMLToPDF
- **I18n**: Custom locale resolver; messages in `src/main/resources/i18n/messages_{en,ja,zh}.properties`

## Developer Commands

```bash
mvn fmt:format             # Format all Java code (Google Java Style) — also runs in pre-commit hook
mvn compile                # Compile (triggers MapStruct + Lombok annotation processors)
mvn test                   # Run all tests
mvn -Dtest=... test        # Single test class
mvn -Dpassword=xxx -Dtest=PasswordEncoderTest#generateHashForCustomPassword test  # Generate BCrypt hash
mvn spring-boot:run        # Start application
```

Pre-commit hook (`.githooks/pre-commit`) auto-formats Java files then restages them. Run `mvn validate` once to set `core.hooksPath`.

## Local Setup

1. `cp .env.example .env.local` — fill in DB/Redis/JWT values
2. `JWT_SECRET` must be **≥ 32 characters**
3. Run via VSCode launch config (`Run Application`) — already references `.env.local`
4. App runs on **port 8080**; Actuator on **port 9090**

Required env vars: `DB_HOST`, `DB_PORT`, `DB_SCHEMA`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, `JWT_SECRET`

## Architecture

Package root: `com.lincsoft`

| Directory | Purpose |
|---|---|
| `controller/` | REST endpoints (`auth/`, `master/`, `log/`, `common/`) |
| `service/` | Business logic (`auth/`, `master/`, `system/`, `report/`, `common/`) |
| `mapper/` | MyBatis-Plus mappers (`master/`, `system/`, `report/`) |
| `mapstruct/` | Bean mapping interfaces (Lombok + MapStruct run in that order via Maven AP) |
| `filter/` | Servlet filter chain (order matters): TraceIdFilter → ContentCachingFilter → RateLimitFilter → IpBlacklistFilter → JwtAuthorizationFilter → PreAuthenticationChecks |
| `config/` | Spring config classes: `SecurityConfig`, `WebMvcConfig`, `RedisConfig`, `MyBatisPlusConfig`, `AsyncConfig`, `I18nConfig`, `OpenApiConfig`, `PdfConfig`, `AppProperties` |
| `common/` | Base classes (`BaseEntity`, `Result`, `PageRequest`) |
| `constant/` | Enums (`RoleCodeEnums`, `UserStatusEnum`, `Module`, `OperationType`) |
| `util/` | Utilities (`JwtUtil`, `IpChecker`, `LogUtil`) |
| `aspect/` | `OperationLogAspect` — @OperationLog annotation handler |
| `advice/` | `GlobalResponseAdvice` — wraps all responses in `Result<T>` |
| `exception/` | `GlobalExceptionHandler` + `BusinessException` |
| `i18n/` | Locale resolution + `MessageService` |
| `dto/` | Data transfer objects and view models |

## Key Conventions

- **All API responses** wrapped in `Result<T>` (use `@IgnoreResultWrapper` to opt out)
- **Optimistic locking** via `version` field on all entities extending `VersionedEntity`
- **Logical delete**: `deleted` field (1=deleted, 0=active) — MyBatisPlus auto-filters
- **ADMIN grants all authorities**: `CustomMethodSecurityExpressionHandler` — ADMIN users pass any `@PreAuthorize`
- **Access token** in `Authorization: Bearer` header; **refresh token** in HttpOnly cookie
- **Cache eviction**: update/delete operations evict `UserDetails` cache in Redis
- **API docs** at `/swagger-ui.html` or `/doc.html` (dev only)
- **No Spring context tests exist** — only `PasswordEncoderTest` (standalone BCrypt test)

## Logging

Logback config at `logback-spring.xml` — four separate files in `logs/`:
- `app.log` — all levels (30-day retention)
- `access.log` — HTTP access (30-day retention, 10GB cap)
- `error.log` — ERROR only (90-day retention)
- `operation.log` — business audit (90-day retention)

All logs include `traceId` and `username` from MDC. Console output only in dev/test.

## DB

- DDL in `src/main/resources/db/` — `system.sql` (logs), `master.sql`
- Optional MySQL RANGE partitioning for log tables — see `db/partition/` and README

## Nginx Proxy

Must use `proxy_set_header X-Forwarded-For $remote_addr` (not `$proxy_add_x_forwarded_for`) to prevent IP spoofing. Affects rate limiting and access logs.
