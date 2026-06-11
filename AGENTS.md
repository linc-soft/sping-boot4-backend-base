# AGENTS.md — Spring Boot 4 Backend Base

## Stack

- **Spring Boot 4.0.5 + Java 25** — Maven single-module project. `Application.java` uses Java 25 implicit class: `void main(String[] args)` (no `public static`), `@EnableScheduling`.
- **MyBatisPlus 3.5.16** — ORM with logical delete (`deleted`, 1=deleted, 0=active). Use `MPJBaseMapper` (from mybatis-plus-join) for join queries instead of `BaseMapper`.
- **Spring Security + JWT (jjwt 0.13.0)** — Stateless auth; refresh via HttpOnly cookie.
- **MySQL + Redis** — Required for dev. All connection settings via env vars (`DB_*`, `REDIS_*`).
- **MapStruct 1.6.3 + Lombok** — `lombok-mapstruct-binding` ensures Lombok runs before MapStruct in annotation processing.
- **OpenHTMLToPDF 1.0.10** — PDF generation from Thymeleaf templates.

## Commands

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # run dev server
mvn fmt:format && mvn compile                         # validate before commit
mvn clean package -DskipTests                         # build
mvn -Dtest=PasswordEncoderTest#generateHashForCustomPassword -Dpassword=yourPassword test  # BCrypt hash
```

Active profile defaults to `dev` via `<active.profile>` in `pom.xml`. The VSCode launch config (`Run Application`) reads `.env.local` via `envFile`.

## Code Style

- **Google Java Format** enforced by `fmt-maven-plugin`. Pre-commit hook (`.githooks/pre-commit`) auto-runs `mvn fmt:format` and re-stages `.java` files.
- Hooks path auto-configured via `maven-antrun-plugin` on `mvn validate`.
- **Always run `mvn fmt:format` before committing** if the hook isn't active.

## Architecture

```
com.lincsoft/
├── Application.java           # @SpringBootApplication + @EnableScheduling
├── config/                    # Security, MyBatisPlus, Redis, Async configs
├── controller/                # REST controllers by module (auth/master/log/...)
│   └── */vo/                  # Request/response DTOs per controller (PageRequest, PageResponseItem, DetailResponse)
├── services/                  # Business logic by module (master/system/report/...)
├── entity/                    # MyBatisPlus entities (master/*, system/*)
├── mapper/                    # MyBatisPlus data mappers (NOT MapStruct)
├── mapstruct/                 # MapStruct DTO converters (NOT MyBatisPlus)
├── aspect/                    # Operation logging aspect
├── filter/                    # Rate limiting, CSRF, JWT filters
├── interceptor/               # AccessLogInterceptor, SqlLogInterceptor
├── common/                    # Result<T>, PageRequest, BaseEntity, SelectOption
├── constant/                  # Enums (Module, SubModule, MessageEnums, RoleCodeEnums)
├── util/                      # JwtUtil, LogUtil, IpChecker
├── event/                     # Spring events (e.g. UserCreatedEvent)
├── advice/                    # @ControllerAdvice (GlobalResponseAdvice, GlobalExceptionHandler)
├── annotation/                # Custom annotations (@IgnoreResultWrapper)
└── i18n/                      # Internationalization support
```

## Key Patterns

### API Response
All responses wrapped in `Result<T>` (`code`, `messageKey`, `messageArgs`, `message`, `data`). `messageKey` resolved at serialization by `GlobalResponseAdvice`. Use `@IgnoreResultWrapper` to bypass wrapping.

### Security Endpoint Whitelist
Configured in `application.yml` under `app.security.public-endpoints` (no auth, no CSRF) and `app.security.auth-only-whitelist` (CSRF only, no auth). Dev profile adds Swagger/OpenAPI to public.

### Log System — Async Batch Persistence
Log services (AccessLog, ErrorLog, OperationLog, SqlLog) use `ConcurrentLinkedQueue` buffer + `@Scheduled` batch flush. Config per log type: `batch-size`, `flush-interval-ms`, `max-batches-per-flush`. These are NOT blocking — enqueue never throws.

### SqlLogInterceptor — MyBatis Native Plugin
Registered via `@Bean` in `MyBatisPlusConfig`, NOT via `MybatisPlusInterceptor.addInnerInterceptor()`. Uses `@Intercepts` on `Executor.query/update`. Self-exclusion: checks `MappedStatement.getId()` contains configured exclude mapper class names.

### Database
- **DDL**: `db/system.sql` (core), `db/master.sql`, `db/oa.sql`, `db/partition/*.sql` (optional partition DDL + maintenance procs).
- Logical delete globally configured (`deleted` field).
- All entities extend `BaseEntity` (provides `id`, `createTime`, `updateTime`).

### File Uploads
Stored in `./uploads/` (configurable via `UPLOAD_DIR` env var), gitignored.

## Important Constraints

### Reverse Proxy — X-Forwarded-For
The app uses `X-Forwarded-For` for rate limiting and access logs. **Must overwrite, not append**. In Nginx:
```nginx
proxy_set_header X-Forwarded-For $remote_addr;  # NOT $proxy_add_x_forwarded_for
```

### Management Port Isolation
Actuator on **port 9090** (business = 8080). Never expose to public internet.

### Test Coverage
Only one test class exists (`PasswordEncoderTest`). No test framework beyond `spring-boot-starter-test`. Do not add tests unless asked.

## OpenCode Config

`opencode.json` grants cross-project access to `D:/Projects/github/vue3-vuetify4-frontend-base/` and loads its `AGENTS.md` as merged instructions. Respect both when working on full-stack features.
