# API Reference

## Authentication

### Login

| Item         | Detail                              |
| ------------ | ----------------------------------- |
| **Endpoint** | `POST /api/auth/login`              |
| **Auth**     | Public (No authentication required) |

Authenticates user credentials via Spring Security's AuthenticationManager. On success, generates a JWT access token (returned in response body) and a refresh token (set as HttpOnly cookie). Includes brute-force protection: records login failures per account and per IP, locks account after threshold exceeded, auto-blocks IP for non-whitelisted addresses. Registers active session in Redis (kicks off previous session if exists).

---

### Refresh Token

| Item         | Detail                                     |
| ------------ | ------------------------------------------ |
| **Endpoint** | `POST /api/auth/refresh`                   |
| **Auth**     | CSRF protected, no authentication required |

Implements Token Rotation. Reads the refresh token from HttpOnly cookie, validates it (type, expiration, revocation status), revokes the old refresh token, reloads user details to verify the user is still active, generates a new token pair, and updates the active session in Redis.

---

### Logout

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `POST /api/auth/logout`        |
| **Auth**     | Authenticated + CSRF protected |

Revokes both the access token (from Authorization header) and refresh token (from cookie) by adding their JTIs to the Redis blacklist. Clears the refresh token cookie, removes the active session record from Redis, and evicts the UserDetails cache.

---

## Role Management

### Get Role

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `GET /api/roles/{id}`           |
| **Auth**     | Requires `ROLE_READ` permission |

Retrieves a single role by ID from the database. Returns role details including ID, name, code, description, and version. Throws 404 if not found.

---

### Get Role List

| Item         | Detail                                                                               |
| ------------ | ------------------------------------------------------------------------------------ |
| **Endpoint** | `GET /api/roles`                                                                     |
| **Auth**     | Requires `ROLE_READ` permission                                                      |
| **Params**   | `roleName` (partial match), `roleCode` (prefix match), `description` (partial match) |

Queries roles by optional conditions (name partial match, code prefix match, description partial match). Returns a list of roles with their direct parent role IDs and version (for optimistic locking on delete), resolved in a single batch query to avoid N+1 problems. Results ordered by update time descending.

---

### Create Role

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `POST /api/roles`              |
| **Auth**     | Requires `ROLE_WRITE` permission |

Creates a new role with the provided name, description, and optional parent role IDs for inheritance. Role code is not required - it's only used for base roles. Custom roles inherit permissions from their parent roles. Returns the created role ID.

**Request Body:**

- `roleName` (required): Role name, max 64 characters
- `description` (optional): Role description, max 255 characters
- `parentRoleIds` (optional): List of parent role IDs for inheritance

---

### Update Role

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `PUT /api/roles`                |
| **Auth**     | Requires `ROLE_WRITE` permission |

Updates an existing role's name, description, and inheritance relationships. Uses optimistic locking via version field to ensure concurrency safety. Throws exception if the role was modified by another transaction.

**Request Body:**

- `id` (required): Role ID
- `roleName` (required): Role name, max 64 characters
- `description` (optional): Role description, max 255 characters
- `parentRoleIds` (optional): List of parent role IDs for inheritance (replaces existing relationships)
- `version` (required): Version for optimistic locking

---

### Delete Role

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `DELETE /api/roles`            |
| **Auth**     | Requires `ROLE_DELETE` permission |

Deletes a role after validating it is not assigned to any user and not inherited by other roles. Uses optimistic locking. After deletion, cleans up all inheritance relationships where this role is a child.

**Request Body:**

- `id` (required): Role ID
- `version` (required): Version for optimistic locking

---

## User Management

### Get User

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `GET /api/users/{id}`           |
| **Auth**     | Requires `USER_READ` permission |

Retrieves a single user by ID. Returns user details including ID, username, status, version, and the list of directly assigned role IDs (`roleIds`, never null; empty when the user has no roles). Throws 404 if not found.

---

### Get User List

| Item         | Detail                                             |
| ------------ | -------------------------------------------------- |
| **Endpoint** | `GET /api/users`                                   |
| **Auth**     | Requires `USER_READ` permission                    |
| **Params**   | `username` (partial match), `status` (exact match) |

Queries users by optional conditions (username partial match, status exact match). Returns a list with version (for optimistic locking on delete/update), ordered by update time descending.

---

### Get User Page

| Item         | Detail                                                             |
| ------------ | ------------------------------------------------------------------ |
| **Endpoint** | `GET /api/users/page`                                              |
| **Auth**     | Requires `USER_READ` permission                                    |
| **Params**   | `page`, `size`, `username` (partial match), `status` (exact match) |

Queries users with pagination support. Accepts page number, page size, and optional filter conditions. Returns paged results with total count. Each item includes version for optimistic locking on delete/update.

---

### Create User

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `POST /api/users`              |
| **Auth**     | Requires `USER_WRITE` permission |

Creates a new user with username uniqueness validation. Password is encrypted via PasswordEncoder before storage. Supports simultaneous role assignment via roleIds. Returns the created user ID.

**Request Body:**

- `username` (required): Username, alphanumeric and underscore only (`^[a-zA-Z0-9_]+$`), max 64 characters
- `password` (required): Password, 8–128 characters
- `status` (required): User status (`0` inactive / `1` active), must be a valid `UserStatusEnum` value
- `roleIds` (optional): List of role IDs to assign

---

### Update User

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `PUT /api/users`                |
| **Auth**     | Requires `USER_WRITE` permission |

Updates user information. Username cannot be modified. Password is encrypted if provided, otherwise kept unchanged. Uses optimistic locking via version field. When `roleIds` is provided, synchronizes the user's role assignments against the list (adds missing, revokes extra); when omitted (null), existing roles remain unchanged. Evicts UserDetails cache after update.

**Request Body:**

- `id` (required): User ID
- `username` (optional): Username (read-only, must equal current value), alphanumeric and underscore only, max 64 characters
- `password` (optional): New password, 8–128 characters; omit to keep unchanged
- `status` (optional): User status (`0` inactive / `1` active)
- `roleIds` (optional): Target role ID list; omit to keep current roles, pass `[]` to revoke all
- `version` (required): Version for optimistic locking

---

### Delete User

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `DELETE /api/users`            |
| **Auth**     | Requires `USER_DELETE` permission |

Deletes a user with optimistic locking. Evicts UserDetails cache after deletion to ensure consistency.

**Request Body:**

- `id` (required): User ID
- `version` (required): Version for optimistic locking

---

### Generate User List Report

| Item         | Detail                                              |
| ------------ | --------------------------------------------------- |
| **Endpoint** | `GET /api/users/report`                             |
| **Auth**     | Requires `USER_EXPORT` permission                |
| **Params**   | `username` (partial match), `groupBy` (optional)    |

Generates a PDF report of users matching the username filter. The `groupBy` parameter controls how users are grouped in the report:

- **null or blank** — No grouping; all matching users are listed in a single table.
- **`role`** — Groups users by their directly assigned roles. A user with multiple roles appears in each role's group, and each group starts on a new page.
- **`baseRole`** — Groups users by base roles (ancestor roles in the inheritance chain). For each user, all ancestor roles of the user's direct roles are collected. A user with multiple base roles appears in each base role's group, and each group starts on a new page. If a direct role has no ancestors, the user appears under that direct role's group.

Returns a PDF file with `Content-Type: application/pdf` and `Content-Disposition: attachment` header. Report labels are automatically localized based on the client's `Accept-Language` header. The maximum number of exported records is limited by configuration (`app.report.max-export-records`, default 10,000).

---

## Common

### Get Enum List

| Item         | Detail                                                |
| ------------ | ----------------------------------------------------- |
| **Endpoint** | `GET /api/common/enums`                               |
| **Auth**     | Public (No authentication required)                   |
| **Params**   | `type` — supported values: `user-status`, `role-code`, `module`, `sub-module` |

Returns enumeration data list by type. Supports `user-status` (user status options), `role-code` (role code options), `module` (module options), and `sub-module` (sub-module options). Each item contains `code` and `name` fields.

---

### Get Select Options

| Item         | Detail                                                                                            |
| ------------ | ------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/common/select-options`                                                                  |
| **Auth**     | Permission determined by `@SelectOptionPermission` annotation on each `SelectOptionProvider`     |
| **Params**   | `type` — dynamically registered via `SelectOptionProvider` implementations (e.g., `role`, `user`) |

Returns select option list for frontend dropdown/select components. Provider implementations are auto-discovered via Spring DI. To add a new type, simply create a new `SelectOptionProvider` bean — no controller modification required. Each option contains `value`, `label`, and `description`.

Permission is checked per provider: if the `SelectOptionProvider` class is annotated with `@SelectOptionPermission`, the user must have the specified authority (e.g., `role` type requires `ROLE_READ`, `user` type requires `USER_READ`); providers without the annotation are public.

---

## Log System

### Get Access Log Page

| Item         | Detail                                                                                                                                      |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/logs/access/page`                                                                                                                 |
| **Auth**     | Requires `LOG_READ` permission                                                                                                              |
| **Params**   | `page`, `size`, `traceId` (exact match), `username` (partial match), `method`, `path` (partial match), `statusCode`, `startTime`, `endTime` |

Queries access logs with pagination support. Results ordered by creation time descending. Each item includes: `id`, `traceId`, `username`, `method`, `path`, `statusCode`, `duration`, `clientIp`, `createdAt`.

---

### Get Access Log Detail

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `GET /api/logs/access/{id}`    |
| **Auth**     | Requires `LOG_READ` permission |

Retrieves complete access log information by ID. Returns: `id`, `traceId`, `username`, `method`, `path`, `queryString`, `requestBody`, `responseBody`, `statusCode`, `duration`, `clientIp`, `userAgent`, `createdAt`. Throws 404 if not found.

---

### Get Access Log by TraceId

| Item         | Detail                                 |
| ------------ | -------------------------------------- |
| **Endpoint** | `GET /api/logs/access/trace/{traceId}` |
| **Auth**     | Requires `LOG_READ` permission         |

Retrieves access log by trace ID. Useful for debugging request chains. Throws 404 if not found.

---

### Export Access Logs

| Item         | Detail                           |
| ------------ | -------------------------------- |
| **Endpoint** | `GET /api/logs/access/export`    |
| **Auth**     | Requires `LOG_EXPORT` permission |

Exports access logs matching the query conditions as a CSV file. Limited to 10,000 records to avoid memory issues. Returns `text/csv` content type with UTF-8 BOM for Excel compatibility.

---

### Get Error Log Page

| Item         | Detail                                                                                                                            |
| ------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/logs/error/page`                                                                                                        |
| **Auth**     | Requires `LOG_READ` permission                                                                                                    |
| **Params**   | `page`, `size`, `traceId` (exact match), `keyword` (multi-field fuzzy search), `username` (partial match), `startTime`, `endTime` |

Queries error logs with pagination support. Results ordered by creation time descending. Each item includes: `id`, `traceId`, `errorType`, `message`, `username`, `createdAt`. The `keyword` parameter searches across: `exception_file`, `exception_class`, `exception_method`, `exception_message`, `root_cause_message`, `stack_trace` (OR condition).

---

### Get Error Log Detail

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `GET /api/logs/error/{id}`     |
| **Auth**     | Requires `LOG_READ` permission |

Retrieves complete error log information by ID. Returns: `id`, `traceId`, `errorType`, `message`, `stackTrace`, `username`, `requestMethod`, `requestPath`, `requestBody`, `createdAt`. Throws 404 if not found.

---

### Get Error Log by TraceId

| Item         | Detail                                |
| ------------ | ------------------------------------- |
| **Endpoint** | `GET /api/logs/error/trace/{traceId}` |
| **Auth**     | Requires `LOG_READ` permission        |

Retrieves error log by trace ID. Returns null if no error occurred during the request.

---

### Get Operation Log Page

| Item         | Detail                                                                                                                                                              |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/logs/operation/page`                                                                                                                                      |
| **Auth**     | Requires `LOG_READ` permission                                                                                                                                      |
| **Params**   | `page`, `size`, `traceId` (exact match), `operationType`, `module` (partial match), `subModule` (partial match), `username` (partial match), `startTime`, `endTime` |

Queries operation logs with pagination support. Results ordered by creation time descending. Each item includes: `id`, `traceId`, `module`, `subModule`, `operationType`, `description`, `duration`, `username`, `createdAt`.

---

### Get Operation Log Detail

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `GET /api/logs/operation/{id}` |
| **Auth**     | Requires `LOG_READ` permission |

Retrieves complete operation log information by ID. Returns: `id`, `traceId`, `module`, `subModule`, `operationType`, `description`, `duration`, `requestMethod`, `requestUrl`, `clientIp`, `username`, `createdAt`. Throws 404 if not found.

---

### Get Operation Logs by TraceId

| Item         | Detail                                    |
| ------------ | ----------------------------------------- |
| **Endpoint** | `GET /api/logs/operation/trace/{traceId}` |
| **Auth**     | Requires `LOG_READ` permission            |

Retrieves all operation logs for a specific trace ID. Results ordered by creation time ascending (chronological order). Useful for viewing the operation sequence within a single request.

---

### Get Module List

| Item         | Detail                            |
| ------------ | --------------------------------- |
| **Endpoint** | `GET /api/logs/operation/modules` |
| **Auth**     | Requires `LOG_READ` permission    |

Returns distinct module names for filtering dropdown. Only includes modules that have at least one operation log entry.

---

### Get Trace Detail

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `GET /api/logs/trace/{traceId}` |
| **Auth**     | Requires `LOG_READ` permission  |

Retrieves complete trace information by trace ID. Combines:

- **Access Log**: The original request details
- **Error Log**: Error information if the request failed (null otherwise)
- **Operation Logs**: List of all operations performed during this request

This endpoint provides a complete view of a request's lifecycle for debugging and auditing purposes.

---

---

# API 参考文档

## 认证

### 登录

| 项目         | 详情                   |
| ------------ | ---------------------- |
| **接口地址** | `POST /api/auth/login` |
| **权限**     | 公开（无需认证）       |

通过 Spring Security 的 AuthenticationManager 验证用户凭证。认证成功后生成 JWT 访问令牌（在响应体中返回）和刷新令牌（通过 HttpOnly Cookie 设置）。包含暴力破解防护：按账户和 IP 两个维度记录登录失败，超过阈值后锁定账户，非白名单 IP 自动封禁。在 Redis 中注册活跃会话（若已有会话则踢出旧会话）。

---

### 刷新令牌

| 项目         | 详情                     |
| ------------ | ------------------------ |
| **接口地址** | `POST /api/auth/refresh` |
| **权限**     | CSRF 保护，无需认证      |

实现令牌轮换机制。从 HttpOnly Cookie 中读取刷新令牌，验证其有效性（类型、过期时间、撤销状态），撤销旧的刷新令牌，重新加载用户信息以验证用户仍处于活跃状态，生成新的令牌对，并在 Redis 中更新活跃会话。

---

### 登出

| 项目         | 详情                    |
| ------------ | ----------------------- |
| **接口地址** | `POST /api/auth/logout` |
| **权限**     | 需要认证 + CSRF 保护    |

通过将 JTI 添加到 Redis 黑名单来撤销访问令牌（来自 Authorization 头）和刷新令牌（来自 Cookie）。清除刷新令牌 Cookie，从 Redis 移除活跃会话记录，并清除 UserDetails 缓存。

---

## 角色管理

### 获取角色详情

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `GET /api/roles/{id}` |
| **权限**     | 需要 `ROLE_READ` 权限 |

根据 ID 从数据库中获取单个角色。返回角色详情，包括 ID、名称、代码、描述和版本号。未找到时抛出 404 异常。

---

### 获取角色列表

| 项目         | 详情                                                                      |
| ------------ | ------------------------------------------------------------------------- |
| **接口地址** | `GET /api/roles`                                                          |
| **权限**     | 需要 `ROLE_READ` 权限                                                     |
| **参数**     | `roleName`（模糊匹配）、`roleCode`（前缀匹配）、`description`（模糊匹配） |

根据可选条件查询角色（名称模糊匹配、代码前缀匹配、描述模糊匹配）。返回角色列表，包含各角色的直接父角色 ID 和版本号（用于删除时的乐观锁），通过单次批量查询解析以避免 N+1 问题。结果按更新时间降序排列。

---

### 创建角色

| 项目         | 详情                 |
| ------------ | -------------------- |
| **接口地址** | `POST /api/roles`    |
| **权限**     | 需要 `ROLE_WRITE` 权限 |

使用提供的名称、描述和可选的父角色 ID 创建新角色。角色代码不是必需的 - 它仅用于基础角色。自定义角色通过继承父角色获得权限。返回创建的角色 ID。

**请求体：**

- `roleName`（必填）：角色名称，最长 64 字符
- `description`（可选）：角色描述，最长 255 字符
- `parentRoleIds`（可选）：父角色 ID 列表，用于继承关系

---

### 更新角色

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `PUT /api/roles`      |
| **权限**     | 需要 `ROLE_WRITE` 权限 |

更新现有角色的名称、描述和继承关系。通过版本字段使用乐观锁确保并发安全。如果角色被其他事务修改则抛出异常。

**请求体：**

- `id`（必填）：角色 ID
- `roleName`（必填）：角色名称，最长 64 字符
- `description`（可选）：角色描述，最长 255 字符
- `parentRoleIds`（可选）：父角色 ID 列表，用于继承关系（替换现有关系）
- `version`（必填）：版本号，用于乐观锁

---

### 删除角色

| 项目         | 详情                 |
| ------------ | -------------------- |
| **接口地址** | `DELETE /api/roles`  |
| **权限**     | 需要 `ROLE_DELETE` 权限 |

验证角色未被任何用户使用且未被其他角色继承后删除角色。使用乐观锁。删除后清理该角色作为子角色的所有继承关系。

**请求体：**

- `id`（必填）：角色 ID
- `version`（必填）：版本号，用于乐观锁

---

## 用户管理

### 获取用户详情

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `GET /api/users/{id}` |
| **权限**     | 需要 `USER_READ` 权限 |

根据 ID 获取单个用户。返回用户详情，包括 ID、用户名、状态、版本号，以及直接分配的角色 ID 列表（`roleIds`，不为 null；用户无角色时为空数组）。未找到时抛出 404 异常。

---

### 获取用户列表

| 项目         | 详情                                         |
| ------------ | -------------------------------------------- |
| **接口地址** | `GET /api/users`                             |
| **权限**     | 需要 `USER_READ` 权限                        |
| **参数**     | `username`（模糊匹配）、`status`（精确匹配） |

根据可选条件查询用户（用户名模糊匹配、状态精确匹配）。返回按更新时间降序排列的列表，每项包含版本号（用于删除/更新时的乐观锁）。

---

### 获取用户分页

| 项目         | 详情                                                         |
| ------------ | ------------------------------------------------------------ |
| **接口地址** | `GET /api/users/page`                                        |
| **权限**     | 需要 `USER_READ` 权限                                        |
| **参数**     | `page`、`size`、`username`（模糊匹配）、`status`（精确匹配） |

支持分页查询用户。接受页码、页大小和可选过滤条件。返回带有总数的分页结果，每项包含版本号（用于删除/更新时的乐观锁）。

---

### 创建用户

| 项目         | 详情                 |
| ------------ | -------------------- |
| **接口地址** | `POST /api/users`    |
| **权限**     | 需要 `USER_WRITE` 权限 |

创建新用户，验证用户名唯一性。密码通过 PasswordEncoder 加密后存储。支持通过 roleIds 同时分配角色。返回创建的用户 ID。

**请求体：**

- `username`（必填）：用户名，仅允许字母、数字和下划线（`^[a-zA-Z0-9_]+$`），最长 64 字符
- `password`（必填）：密码，8–128 字符
- `status`（必填）：用户状态（`0` 禁用 / `1` 启用），必须为合法的 `UserStatusEnum` 值
- `roleIds`（可选）：要分配的角色 ID 列表

---

### 更新用户

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `PUT /api/users`      |
| **权限**     | 需要 `USER_WRITE` 权限 |

更新用户信息。用户名不可修改。如果提供密码则加密存储，否则保持不变。通过版本字段使用乐观锁。当传入 `roleIds` 时，按列表同步用户的角色分配（新增缺失的、移除多余的）；未传入（null）时保持现有角色不变。更新后清除 UserDetails 缓存。

**请求体：**

- `id`（必填）：用户 ID
- `username`（可选）：用户名（只读，需与当前值一致），仅允许字母、数字和下划线，最长 64 字符
- `password`（可选）：新密码，8–128 字符；未传则保持不变
- `status`（可选）：用户状态（`0` 禁用 / `1` 启用）
- `roleIds`（可选）：目标角色 ID 列表；未传保持现有角色，传 `[]` 将移除所有角色
- `version`（必填）：版本号，用于乐观锁

---

### 删除用户

| 项目         | 详情                 |
| ------------ | -------------------- |
| **接口地址** | `DELETE /api/users`  |
| **权限**     | 需要 `USER_DELETE` 权限 |

使用乐观锁删除用户。删除后清除 UserDetails 缓存以确保一致性。

**请求体：**

- `id`（必填）：用户 ID
- `version`（必填）：版本号，用于乐观锁

---

### 生成用户列表报表

| 项目         | 详情                                                    |
| ------------ | ------------------------------------------------------- |
| **接口地址** | `GET /api/users/report`                                 |
| **权限**     | 需要 `USER_EXPORT` 权限                               |
| **参数**     | `username`（模糊匹配）、`groupBy`（可选）               |

根据用户名筛选条件生成用户列表 PDF 报表。`groupBy` 参数控制报表中用户的分组方式：

- **null 或空** — 无分组；所有匹配用户以单一表格列出。
- **`role`** — 按用户直接分配的角色分组。拥有多个角色的用户会出现在每个角色分组中，每个分组从新页开始。
- **`baseRole`** — 按基础角色（继承链中的祖先角色）分组。对于每个用户，收集其直接角色的所有祖先角色，用户会出现在每个基础角色分组中，每个分组从新页开始。如果直接角色没有祖先角色，则用户出现在该直接角色的分组中。

返回 PDF 文件，`Content-Type` 为 `application/pdf`，附带 `Content-Disposition: attachment` 头。报表标签根据客户端 `Accept-Language` 请求头自动本地化。导出记录数上限由配置控制（`app.report.max-export-records`，默认 10,000）。

---

## 公共接口

### 获取枚举列表

| 项目         | 详情                                        |
| ------------ | ------------------------------------------- |
| **接口地址** | `GET /api/common/enums`                     |
| **权限**     | 公开（无需认证）                            |
| **参数**     | `type` — 支持值：`user-status`、`role-code`、`module`、`sub-module` |

根据类型返回枚举数据列表。支持 `user-status`（用户状态选项）、`role-code`（角色代码选项）、`module`（模块选项）和 `sub-module`（子模块选项）。每项包含 `code` 和 `name` 字段。

---

### 获取下拉选项

| 项目         | 详情                                                                   |
| ------------ | ---------------------------------------------------------------------- |
| **接口地址** | `GET /api/common/select-options`                                       |
| **权限**     | 由各 `SelectOptionProvider` 上的 `@SelectOptionPermission` 注解决定     |
| **参数**     | `type` — 通过 `SelectOptionProvider` 实现动态注册（如 `role`、`user`） |

返回前端下拉/选择组件的选项列表。Provider 实现通过 Spring DI 自动发现。添加新类型只需创建新的 `SelectOptionProvider` Bean，无需修改控制器。每个选项包含 `value`、`label` 和 `description`。

权限按 Provider 逐一检查：如果 `SelectOptionProvider` 类标注了 `@SelectOptionPermission`，则用户必须拥有指定权限（如 `role` 类型需要 `ROLE_READ`，`user` 类型需要 `USER_READ`）；未标注注解的 Provider 为公开访问。

---

## 日志系统

### 获取访问日志分页

| 项目         | 详情                                                                                                                              |
| ------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| **接口地址** | `GET /api/logs/access/page`                                                                                                       |
| **权限**     | 需要 `LOG_READ` 权限                                                                                                              |
| **参数**     | `page`、`size`、`traceId`（精确匹配）、`username`（模糊匹配）、`method`、`path`（模糊匹配）、`statusCode`、`startTime`、`endTime` |

分页查询访问日志。结果按创建时间降序排列。每项包含：`id`、`traceId`、`username`、`method`、`path`、`statusCode`、`duration`、`clientIp`、`createdAt`。

---

### 获取访问日志详情

| 项目         | 详情                        |
| ------------ | --------------------------- |
| **接口地址** | `GET /api/logs/access/{id}` |
| **权限**     | 需要 `LOG_READ` 权限        |

根据 ID 获取访问日志完整信息。返回：`id`、`traceId`、`username`、`method`、`path`、`queryString`、`requestBody`、`responseBody`、`statusCode`、`duration`、`clientIp`、`userAgent`、`createdAt`。未找到时抛出 404 异常。

---

### 按 TraceId 获取访问日志

| 项目         | 详情                                   |
| ------------ | -------------------------------------- |
| **接口地址** | `GET /api/logs/access/trace/{traceId}` |
| **权限**     | 需要 `LOG_READ` 权限                   |

根据链路追踪 ID 获取访问日志。用于调试请求链路。未找到时抛出 404 异常。

---

### 导出访问日志

| 项目         | 详情                          |
| ------------ | ----------------------------- |
| **接口地址** | `GET /api/logs/access/export` |
| **权限**     | 需要 `LOG_EXPORT` 权限        |

导出符合条件的访问日志为 CSV 文件。限制最多 10,000 条记录以避免内存问题。返回 `text/csv` 内容类型，包含 UTF-8 BOM 以兼容 Excel。

---

### 获取错误日志分页

| 项目         | 详情                                                                                                               |
| ------------ | ------------------------------------------------------------------------------------------------------------------ |
| **接口地址** | `GET /api/logs/error/page`                                                                                         |
| **权限**     | 需要 `LOG_READ` 权限                                                                                               |
| **参数**     | `page`、`size`、`traceId`（精确匹配）、`keyword`（多字段模糊搜索）、`username`（模糊匹配）、`startTime`、`endTime` |

分页查询错误日志。结果按创建时间降序排列。每项包含：`id`、`traceId`、`errorType`、`message`、`username`、`createdAt`。`keyword` 参数支持在以下字段中模糊搜索：`exception_file`、`exception_class`、`exception_method`、`exception_message`、`root_cause_message`、`stack_trace`（OR 条件，满足任一字段即可）。

---

### 获取错误日志详情

| 项目         | 详情                       |
| ------------ | -------------------------- |
| **接口地址** | `GET /api/logs/error/{id}` |
| **权限**     | 需要 `LOG_READ` 权限       |

根据 ID 获取错误日志完整信息。返回：`id`、`traceId`、`errorType`、`message`、`stackTrace`、`username`、`requestMethod`、`requestPath`、`requestBody`、`createdAt`。未找到时抛出 404 异常。

---

### 按 TraceId 获取错误日志

| 项目         | 详情                                  |
| ------------ | ------------------------------------- |
| **接口地址** | `GET /api/logs/error/trace/{traceId}` |
| **权限**     | 需要 `LOG_READ` 权限                  |

根据链路追踪 ID 获取错误日志。如果请求未出错则返回 null。

---

### 获取操作日志分页

| 项目         | 详情                                                                                                                                                  |
| ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| **接口地址** | `GET /api/logs/operation/page`                                                                                                                        |
| **权限**     | 需要 `LOG_READ` 权限                                                                                                                                  |
| **参数**     | `page`、`size`、`traceId`（精确匹配）、`operationType`、`module`（模糊匹配）、`subModule`（模糊匹配）、`username`（模糊匹配）、`startTime`、`endTime` |

分页查询操作日志。结果按创建时间降序排列。每项包含：`id`、`traceId`、`module`、`subModule`、`operationType`、`description`、`duration`、`username`、`createdAt`。

---

### 获取操作日志详情

| 项目         | 详情                           |
| ------------ | ------------------------------ |
| **接口地址** | `GET /api/logs/operation/{id}` |
| **权限**     | 需要 `LOG_READ` 权限           |

根据 ID 获取操作日志完整信息。返回：`id`、`traceId`、`module`、`subModule`、`operationType`、`description`、`duration`、`requestMethod`、`requestUrl`、`clientIp`、`username`、`createdAt`。未找到时抛出 404 异常。

---

### 按 TraceId 获取操作日志列表

| 项目         | 详情                                      |
| ------------ | ----------------------------------------- |
| **接口地址** | `GET /api/logs/operation/trace/{traceId}` |
| **权限**     | 需要 `LOG_READ` 权限                      |

根据链路追踪 ID 获取所有操作日志。结果按创建时间升序排列（时间顺序）。用于查看单次请求内的操作序列。

---

### 获取模块列表

| 项目         | 详情                              |
| ------------ | --------------------------------- |
| **接口地址** | `GET /api/logs/operation/modules` |
| **权限**     | 需要 `LOG_READ` 权限              |

返回去重的模块名称列表，用于筛选下拉框。仅包含有操作日志记录的模块。

---

### 获取链路追踪详情

| 项目         | 详情                            |
| ------------ | ------------------------------- |
| **接口地址** | `GET /api/logs/trace/{traceId}` |
| **权限**     | 需要 `LOG_READ` 权限            |

根据链路追踪 ID 获取完整的链路信息。组合返回：

- **访问日志**：原始请求详情
- **错误日志**：请求失败时的错误信息（成功则为 null）
- **操作日志列表**：该请求期间执行的所有操作

此接口提供请求生命周期的完整视图，用于调试和审计。

---

---

# API リファレンス

## 認証

### ログイン

| 項目               | 詳細                   |
| ------------------ | ---------------------- |
| **エンドポイント** | `POST /api/auth/login` |
| **認可**           | 公開（認証不要）       |

Spring Security の AuthenticationManager を介してユーザー認証情報を検証します。認証成功時に JWT アクセストークン（レスポンスボディで返却）とリフレッシュトークン（HttpOnly Cookie で設定）を生成します。ブルートフォース保護機能を含み、アカウントおよび IP ごとにログイン失敗を記録し、閾値超過後にアカウントをロック、ホワイトリスト外の IP を自動ブロックします。Redis にアクティブセッションを登録します（既存セッションがある場合は強制退出）。

---

### トークンリフレッシュ

| 項目               | 詳細                     |
| ------------------ | ------------------------ |
| **エンドポイント** | `POST /api/auth/refresh` |
| **認可**           | CSRF 保護、認証不要      |

トークンローテーションを実装します。HttpOnly Cookie からリフレッシュトークンを読み取り、有効性（タイプ、有効期限、失効状態）を検証し、古いリフレッシュトークンを失効させ、ユーザー情報を再読み込みしてユーザーがアクティブであることを確認し、新しいトークンペアを生成し、Redis のアクティブセッションを更新します。

---

### ログアウト

| 項目               | 詳細                    |
| ------------------ | ----------------------- |
| **エンドポイント** | `POST /api/auth/logout` |
| **認可**           | 認証必須 + CSRF 保護    |

Authorization ヘッダーのアクセストークンと Cookie のリフレッシュトークンの JTI を Redis ブラックリストに追加して失効させます。リフレッシュトークン Cookie をクリアし、Redis からアクティブセッションレコードを削除し、UserDetails キャッシュを無効化します。

---

## ロール管理

### ロール詳細取得

| 項目               | 詳細                   |
| ------------------ | ---------------------- |
| **エンドポイント** | `GET /api/roles/{id}`  |
| **認可**           | `ROLE_READ` 権限が必要 |

ID でデータベースから単一のロールを取得します。ID、名前、コード、説明、バージョンを含むロール詳細を返します。見つからない場合は 404 例外をスローします。

---

### ロール一覧取得

| 項目               | 詳細                                                                      |
| ------------------ | ------------------------------------------------------------------------- |
| **エンドポイント** | `GET /api/roles`                                                          |
| **認可**           | `ROLE_READ` 権限が必要                                                    |
| **パラメータ**     | `roleName`（部分一致）、`roleCode`（前方一致）、`description`（部分一致） |

オプション条件（名前部分一致、コード前方一致、説明部分一致）でロールを検索します。各ロールの直接親ロール ID とバージョン（削除時の楽観的ロック用）を含むリストを返し、N+1 問題を回避するため単一バッチクエリで解決します。結果は更新日時の降順で並べられます。

---

### ロール作成

| 項目               | 詳細                  |
| ------------------ | --------------------- |
| **エンドポイント** | `POST /api/roles`     |
| **認可**           | `ROLE_WRITE` 権限が必要 |

提供された名前、説明、およびオプションの親ロール ID で新しいロールを作成します。ロールコードは必須ではありません - ベースロールのみで使用されます。カスタムロールは親ロールから権限を継承します。作成されたロール ID を返します。

**リクエストボディ：**

- `roleName`（必須）：ロール名、最大 64 文字
- `description`（オプション）：ロールの説明、最大 255 文字
- `parentRoleIds`（オプション）：継承用の親ロール ID リスト

---

### ロール更新

| 項目               | 詳細                   |
| ------------------ | ---------------------- |
| **エンドポイント** | `PUT /api/roles`       |
| **認可**           | `ROLE_WRITE` 権限が必要 |

既存ロールの名前、説明、および継承関係を更新します。バージョンフィールドによる楽観的ロックで並行処理の安全性を確保します。他のトランザクションによって変更された場合は例外をスローします。

**リクエストボディ：**

- `id`（必須）：ロール ID
- `roleName`（必須）：ロール名、最大 64 文字
- `description`（オプション）：ロールの説明、最大 255 文字
- `parentRoleIds`（オプション）：継承用の親ロール ID リスト（既存の関係を置換）
- `version`（必須）：楽観的ロック用のバージョン

---

### ロール削除

| 項目               | 詳細                  |
| ------------------ | --------------------- |
| **エンドポイント** | `DELETE /api/roles`   |
| **認可**           | `ROLE_DELETE` 権限が必要 |

ロールがユーザーに割り当てられておらず、他のロールに継承されていないことを検証後に削除します。楽観的ロックを使用します。削除後、このロールが子ロールである全ての継承関係をクリーンアップします。

**リクエストボディ：**

- `id`（必須）：ロール ID
- `version`（必須）：楽観的ロック用のバージョン

---

## ユーザー管理

### ユーザー詳細取得

| 項目               | 詳細                   |
| ------------------ | ---------------------- |
| **エンドポイント** | `GET /api/users/{id}`  |
| **認可**           | `USER_READ` 権限が必要 |

ID でユーザーを取得します。ID、ユーザー名、ステータス、バージョン、および直接割り当てられたロール ID リスト（`roleIds`、null にならず、ロールがない場合は空配列）を含むユーザー詳細を返します。見つからない場合は 404 例外をスローします。

---

### ユーザー一覧取得

| 項目               | 詳細                                         |
| ------------------ | -------------------------------------------- |
| **エンドポイント** | `GET /api/users`                             |
| **認可**           | `USER_READ` 権限が必要                       |
| **パラメータ**     | `username`（部分一致）、`status`（完全一致） |

オプション条件（ユーザー名部分一致、ステータス完全一致）でユーザーを検索します。各項目にバージョン（削除・更新時の楽観的ロック用）を含み、更新日時の降順でリストを返します。

---

### ユーザーページング取得

| 項目               | 詳細                                                         |
| ------------------ | ------------------------------------------------------------ |
| **エンドポイント** | `GET /api/users/page`                                        |
| **認可**           | `USER_READ` 権限が必要                                       |
| **パラメータ**     | `page`、`size`、`username`（部分一致）、`status`（完全一致） |

ページネーション付きでユーザーを検索します。ページ番号、ページサイズ、オプションのフィルター条件を受け付けます。各項目にバージョン（削除・更新時の楽観的ロック用）を含み、総数を含むページング結果を返します。

---

### ユーザー作成

| 項目               | 詳細                  |
| ------------------ | --------------------- |
| **エンドポイント** | `POST /api/users`     |
| **認可**           | `USER_WRITE` 権限が必要 |

ユーザー名の一意性を検証して新規ユーザーを作成します。パスワードは PasswordEncoder で暗号化して保存します。roleIds による同時ロール割り当てをサポートします。作成されたユーザー ID を返します。

**リクエストボディ：**

- `username`（必須）：ユーザー名、英数字とアンダースコアのみ（`^[a-zA-Z0-9_]+$`）、最大 64 文字
- `password`（必須）：パスワード、8～128 文字
- `status`（必須）：ユーザーステータス（`0` 無効 / `1` 有効）、有効な `UserStatusEnum` 値である必要があります
- `roleIds`（任意）：割り当てるロール ID リスト

---

### ユーザー更新

| 項目               | 詳細                   |
| ------------------ | ---------------------- |
| **エンドポイント** | `PUT /api/users`       |
| **認可**           | `USER_WRITE` 権限が必要 |

ユーザー情報を更新します。ユーザー名は変更できません。パスワードが提供された場合は暗号化して保存し、そうでなければ変更しません。バージョンフィールドによる楽観的ロックを使用します。`roleIds` が指定された場合、ユーザーのロール割り当てをそのリストに同期します（不足分を追加、余分を削除）。省略（null）の場合は既存ロールをそのままとします。更新後に UserDetails キャッシュを無効化します。

**リクエストボディ：**

- `id`（必須）：ユーザー ID
- `username`（任意）：ユーザー名（読み取り専用、現在の値と同じである必要）、英数字とアンダースコアのみ、最大 64 文字
- `password`（任意）：新しいパスワード、8～128 文字。省略時は変更されません
- `status`（任意）：ユーザーステータス（`0` 無効 / `1` 有効）
- `roleIds`（任意）：目標ロール ID リスト。省略時は現在のロールを維持、`[]` を渡すと全て削除
- `version`（必須）：楽観的ロック用のバージョン

---

### ユーザー削除

| 項目               | 詳細                  |
| ------------------ | --------------------- |
| **エンドポイント** | `DELETE /api/users`   |
| **認可**           | `USER_DELETE` 権限が必要 |

楽観的ロックを使用してユーザーを削除します。削除後に UserDetails キャッシュを無効化して整合性を確保します。

**リクエストボディ：**

- `id`（必須）：ユーザー ID
- `version`（必須）：楽観的ロック用のバージョン

---

### ユーザー一覧レポート生成

| 項目               | 詳細                                                      |
| ------------------ | --------------------------------------------------------- |
| **エンドポイント** | `GET /api/users/report`                                   |
| **認可**           | `USER_EXPORT` 権限が必要                                |
| **パラメータ**     | `username`（部分一致）、`groupBy`（オプション）           |

ユーザー名フィルタに一致するユーザーの PDF レポートを生成します。`groupBy` パラメータはレポート内のグループ化方法を制御します：

- **null または空白** — グループ化なし。一致する全ユーザーを単一テーブルで表示します。
- **`role`** — ユーザーの直接割り当てロールでグループ化します。複数ロールを持つユーザーは各ロールグループに表示され、各グループは改ページから開始します。
- **`baseRole`** — ベースロール（継承チェーンの祖先ロール）でグループ化します。各ユーザーの直接ロールの全祖先ロールを収集し、ユーザーは各ベースロールグループに表示されます。各グループは改ページから開始します。直接ロールに祖先がない場合、ユーザーはその直接ロールのグループに表示されます。

`Content-Type: application/pdf` および `Content-Disposition: attachment` ヘッダー付きの PDF ファイルを返します。レポートラベルはクライアントの `Accept-Language` ヘッダーに基づいて自動的にローカライズされます。エクスポートレコード数の上限は設定で制御されます（`app.report.max-export-records`、デフォルト 10,000）。

---

## 共通

### 列挙リスト取得

| 項目               | 詳細                                            |
| ------------------ | ----------------------------------------------- |
| **エンドポイント** | `GET /api/common/enums`                         |
| **認可**           | 公開（認証不要）                                |
| **パラメータ**     | `type` — サポート値：`user-status`、`role-code`、`module`、`sub-module` |

タイプ別に列挙データリストを返します。`user-status`（ユーザーステータスオプション）、`role-code`（ロールコードオプション）、`module`（モジュールオプション）、`sub-module`（サブモジュールオプション）をサポートします。各項目には `code` と `name` フィールドが含まれます。

---

### セレクトオプション取得

| 項目               | 詳細                                                                     |
| ------------------ | ------------------------------------------------------------------------ |
| **エンドポイント** | `GET /api/common/select-options`                                         |
| **認可**           | 各 `SelectOptionProvider` の `@SelectOptionPermission` 注記により決定   |
| **パラメータ**     | `type` — `SelectOptionProvider` 実装により動的登録（例：`role`、`user`） |

フロントエンドのドロップダウン/セレクトコンポーネント用のオプションリストを返します。Provider 実装は Spring DI により自動検出されます。新しいタイプを追加するには、新しい `SelectOptionProvider` Bean を作成するだけで、コントローラーの変更は不要です。各オプションには `value`、`label`、`description` が含まれます。

権限は Provider ごとにチェックされます。`SelectOptionProvider` クラスに `@SelectOptionPermission` が付与されている場合、ユーザーは指定された権限を持っている必要があります（例：`role` タイプには `ROLE_READ`、`user` タイプには `USER_READ`）。注記がない Provider は公開アクセスです。

---

## ログシステム

### アクセスログページング取得

| 項目               | 詳細                                                                                                                              |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| **エンドポイント** | `GET /api/logs/access/page`                                                                                                       |
| **認可**           | `LOG_READ` 権限が必要                                                                                                             |
| **パラメータ**     | `page`、`size`、`traceId`（完全一致）、`username`（部分一致）、`method`、`path`（部分一致）、`statusCode`、`startTime`、`endTime` |

ページネーション付きでアクセスログを検索します。結果は作成日時の降順で並べられます。各項目には `id`、`traceId`、`username`、`method`、`path`、`statusCode`、`duration`、`clientIp`、`createdAt` が含まれます。

---

### アクセスログ詳細取得

| 項目               | 詳細                        |
| ------------------ | --------------------------- |
| **エンドポイント** | `GET /api/logs/access/{id}` |
| **認可**           | `LOG_READ` 権限が必要       |

ID でアクセスログの完全な情報を取得します。`id`、`traceId`、`username`、`method`、`path`、`queryString`、`requestBody`、`responseBody`、`statusCode`、`duration`、`clientIp`、`userAgent`、`createdAt` を返します。見つからない場合は 404 例外をスローします。

---

### TraceId でアクセスログ取得

| 項目               | 詳細                                   |
| ------------------ | -------------------------------------- |
| **エンドポイント** | `GET /api/logs/access/trace/{traceId}` |
| **認可**           | `LOG_READ` 権限が必要                  |

トレース ID でアクセスログを取得します。リクエストチェーンのデバッグに使用します。見つからない場合は 404 例外をスローします。

---

### アクセスログエクスポート

| 項目               | 詳細                          |
| ------------------ | ----------------------------- |
| **エンドポイント** | `GET /api/logs/access/export` |
| **認可**           | `LOG_EXPORT` 権限が必要       |

条件に一致するアクセスログを CSV ファイルとしてエクスポートします。メモリ問題を避けるため最大 10,000 件に制限されます。Excel 互換性のため UTF-8 BOM 付きの `text/csv` コンテンツタイプを返します。

---

### エラーログページング取得

| 項目               | 詳細                                                                                                                           |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| **エンドポイント** | `GET /api/logs/error/page`                                                                                                     |
| **認可**           | `LOG_READ` 権限が必要                                                                                                          |
| **パラメータ**     | `page`、`size`、`traceId`（完全一致）、`keyword`（複数フィールドあいまい検索）、`username`（部分一致）、`startTime`、`endTime` |

ページネーション付きでエラーログを検索します。結果は作成日時の降順で並べられます。各項目には `id`、`traceId`、`errorType`、`message`、`username`、`createdAt` が含まれます。`keyword` パラメータは `exception_file`、`exception_class`、`exception_method`、`exception_message`、`root_cause_message`、`stack_trace` フィールドをあいまい検索します（OR 条件）。

---

### エラーログ詳細取得

| 項目               | 詳細                       |
| ------------------ | -------------------------- |
| **エンドポイント** | `GET /api/logs/error/{id}` |
| **認可**           | `LOG_READ` 権限が必要      |

ID でエラーログの完全な情報を取得します。`id`、`traceId`、`errorType`、`message`、`stackTrace`、`username`、`requestMethod`、`requestPath`、`requestBody`、`createdAt` を返します。見つからない場合は 404 例外をスローします。

---

### TraceId でエラーログ取得

| 項目               | 詳細                                  |
| ------------------ | ------------------------------------- |
| **エンドポイント** | `GET /api/logs/error/trace/{traceId}` |
| **認可**           | `LOG_READ` 権限が必要                 |

トレース ID でエラーログを取得します。リクエストがエラーで終了しなかった場合は null を返します。

---

### 操作ログページング取得

| 項目               | 詳細                                                                                                                                                  |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| **エンドポイント** | `GET /api/logs/operation/page`                                                                                                                        |
| **認可**           | `LOG_READ` 権限が必要                                                                                                                                 |
| **パラメータ**     | `page`、`size`、`traceId`（完全一致）、`operationType`、`module`（部分一致）、`subModule`（部分一致）、`username`（部分一致）、`startTime`、`endTime` |

ページネーション付きで操作ログを検索します。結果は作成日時の降順で並べられます。各項目には `id`、`traceId`、`module`、`subModule`、`operationType`、`description`、`duration`、`username`、`createdAt` が含まれます。

---

### 操作ログ詳細取得

| 項目               | 詳細                           |
| ------------------ | ------------------------------ |
| **エンドポイント** | `GET /api/logs/operation/{id}` |
| **認可**           | `LOG_READ` 権限が必要          |

ID で操作ログの完全な情報を取得します。`id`、`traceId`、`module`、`subModule`、`operationType`、`description`、`duration`、`requestMethod`、`requestUrl`、`clientIp`、`username`、`createdAt` を返します。見つからない場合は 404 例外をスローします。

---

### TraceId で操作ログリスト取得

| 項目               | 詳細                                      |
| ------------------ | ----------------------------------------- |
| **エンドポイント** | `GET /api/logs/operation/trace/{traceId}` |
| **認可**           | `LOG_READ` 権限が必要                     |

トレース ID ですべての操作ログを取得します。結果は作成日時の昇順（時系列順）で並べられます。単一リクエスト内の操作シーケンスを表示するために使用します。

---

### モジュールリスト取得

| 項目               | 詳細                              |
| ------------------ | --------------------------------- |
| **エンドポイント** | `GET /api/logs/operation/modules` |
| **認可**           | `LOG_READ` 権限が必要             |

フィルタリング用ドロップダウンのための重複排除されたモジュール名リストを返します。操作ログレコードが存在するモジュールのみが含まれます。

---

### トレース詳細取得

| 項目               | 詳細                            |
| ------------------ | ------------------------------- |
| **エンドポイント** | `GET /api/logs/trace/{traceId}` |
| **認可**           | `LOG_READ` 権限が必要           |

トレース ID で完全なトレース情報を取得します。以下を組み合わせて返します：

- **アクセスログ**：元のリクエスト詳細
- **エラーログ**：リクエスト失敗時のエラー情報（成功時は null）
- **操作ログリスト**：このリクエスト中に実行されたすべての操作

このエンドポイントは、デバッグおよび監査目的でリクエストのライフサイクルの完全なビューを提供します。
