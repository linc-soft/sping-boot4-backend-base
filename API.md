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
| **Auth**     | Requires `VIEW_ROLE` permission |

Retrieves a single role by ID from the database. Returns role details including ID, name, code, description, and version. Throws 404 if not found.

---

### Get Role List

| Item         | Detail                                                                               |
| ------------ | ------------------------------------------------------------------------------------ |
| **Endpoint** | `GET /api/roles`                                                                     |
| **Auth**     | Requires `VIEW_ROLE` permission                                                      |
| **Params**   | `roleName` (partial match), `roleCode` (prefix match), `description` (partial match) |

Queries roles by optional conditions (name partial match, code prefix match, description partial match). Returns a list of roles with their direct parent role IDs and version (for optimistic locking on delete), resolved in a single batch query to avoid N+1 problems. Results ordered by update time descending.

---

### Create Role

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `POST /api/roles`              |
| **Auth**     | Requires `CREATE_ROLE` permission |

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
| **Auth**     | Requires `CREATE_ROLE` permission |

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
| **Auth**     | Requires `DELETE_ROLE` permission |

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
| **Auth**     | Requires `VIEW_USER` permission |

Retrieves a single user by ID. Returns user details including ID, username, status, version, and the list of directly assigned role IDs (`roleIds`, never null; empty when the user has no roles). Throws 404 if not found.

---

### Get User List

| Item         | Detail                                             |
| ------------ | -------------------------------------------------- |
| **Endpoint** | `GET /api/users`                                   |
| **Auth**     | Requires `VIEW_USER` permission                    |
| **Params**   | `username` (partial match), `status` (exact match) |

Queries users by optional conditions (username partial match, status exact match). Returns a list with version (for optimistic locking on delete/update), ordered by update time descending.

---

### Get User Page

| Item         | Detail                                                             |
| ------------ | ------------------------------------------------------------------ |
| **Endpoint** | `GET /api/users/page`                                              |
| **Auth**     | Requires `VIEW_USER` permission                                    |
| **Params**   | `page`, `size`, `username` (partial match), `status` (exact match) |

Queries users with pagination support. Accepts page number, page size, and optional filter conditions. Returns paged results with total count. Each item includes version for optimistic locking on delete/update.

---

### Create User

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `POST /api/users`              |
| **Auth**     | Requires `CREATE_USER` permission |

Creates a new user with username uniqueness validation. Password is encrypted via PasswordEncoder before storage. Supports simultaneous role assignment via roleIds. Returns the created user ID.

**Request Body:**

- `username` (required): Username, alphanumeric and underscore only (`^[a-zA-Z0-9_]+$`), max 64 characters
- `password` (required): Password, 8 E28 characters
- `status` (required): User status (`0` inactive / `1` active), must be a valid `UserStatusType` value
- `roleIds` (optional): List of role IDs to assign

---

### Update User

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `PUT /api/users`                |
| **Auth**     | Requires `CREATE_USER` permission |

Updates user information. Username cannot be modified. Password is encrypted if provided, otherwise kept unchanged. Uses optimistic locking via version field. When `roleIds` is provided, synchronizes the user's role assignments against the list (adds missing, revokes extra); when omitted (null), existing roles remain unchanged. Evicts UserDetails cache after update.

**Request Body:**

- `id` (required): User ID
- `username` (optional): Username (read-only, must equal current value), alphanumeric and underscore only, max 64 characters
- `password` (optional): New password, 8 E28 characters; omit to keep unchanged
- `status` (optional): User status (`0` inactive / `1` active)
- `roleIds` (optional): Target role ID list; omit to keep current roles, pass `[]` to revoke all
- `version` (required): Version for optimistic locking

---

### Delete User

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `DELETE /api/users`            |
| **Auth**     | Requires `DELETE_USER` permission |

Deletes a user with optimistic locking. Evicts UserDetails cache after deletion to ensure consistency.

**Request Body:**

- `id` (required): User ID
- `version` (required): Version for optimistic locking

---

### Generate User List Report

| Item         | Detail                                              |
| ------------ | --------------------------------------------------- |
| **Endpoint** | `GET /api/users/report`                             |
| **Auth**     | Requires `EXPORT_USER` permission                |
| **Params**   | `username` (partial match), `groupBy` (optional)    |

Generates a PDF report of users matching the username filter. The `groupBy` parameter controls how users are grouped in the report:

- **null or blank**  ENo grouping; all matching users are listed in a single table.
- **`role`**  EGroups users by their directly assigned roles. A user with multiple roles appears in each role's group, and each group starts on a new page.
- **`baseRole`**  EGroups users by base roles (ancestor roles in the inheritance chain). For each user, all ancestor roles of the user's direct roles are collected. A user with multiple base roles appears in each base role's group, and each group starts on a new page. If a direct role has no ancestors, the user appears under that direct role's group.

Returns a PDF file with `Content-Type: application/pdf` and `Content-Disposition: attachment` header. Report labels are automatically localized based on the client's `Accept-Language` header. The maximum number of exported records is limited by configuration (`app.report.max-export-records`, default 10,000).

---

## Common

### Get Enum List

| Item         | Detail                                                |
| ------------ | ----------------------------------------------------- |
| **Endpoint** | `GET /api/common/enums`                               |
| **Auth**     | Public (No authentication required)                   |
| **Params**   | `type`  Esupported values: `user-status`, `role-code`, `module`, `sub-module` |

Returns enumeration data list by type. Supports `user-status` (user status options), `role-code` (role code options), `module` (module options), and `sub-module` (sub-module options). Each item contains `code` and `name` fields.

---

### Get Select Options

| Item         | Detail                                                                                            |
| ------------ | ------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/common/select-options`                                                                  |
| **Auth**     | Class-level `@PreAuthorize` with `LIST_OPTIONS` applies to all types                              |
| **Params**   | `type`  Edynamically registered via `SelectOptionProvider` implementations (e.g., `role`, `user`) |

Returns select option list for frontend dropdown/select components. Provider implementations are auto-discovered via Spring DI. To add a new type, simply create a new `SelectOptionProvider` bean  Eno controller modification required. Each option contains `value`, `label`, and `description`.

All `SelectOptionProvider` types require the `LIST_OPTIONS` authority, enforced by the class-level `@PreAuthorize` annotation on `SelectOptionController`.

---

### Upload File

| Item         | Detail                                                                                                      |
| ------------ | ----------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `POST /api/common/files`                                                                                    |
| **Auth**     | Requires `UPLOAD_FILE` permission                                                                            |
| **Params**   | `file` (required)  Emultipart file, `associateType` (optional)  Eassociation type, `associateId` (optional)  Eassociated entity ID |
| **Content-Type** | `multipart/form-data`                                                                                   |

Uploads a single file to the server. The file is stored under a date-based subdirectory (`yyyy/MM/dd`) with a UUID-based filename to prevent conflicts and path traversal attacks. MD5 hash is computed during upload via `DigestInputStream` and stored in the database for integrity verification. File type is auto-detected from extension (1=image, 2=document, 3=archive, 9=other). Extension must be in the allowed list configured in `app.upload.allowed-extensions`. File size must not exceed `app.upload.max-file-size-mb`.

Returns: `id`, `storedName`, `originalFilename`, `datePath`, `dateUrl`, `size`, `contentType`, `md5`.

---

### Upload Files (Batch)

| Item         | Detail                                                                                                          |
| ------------ | --------------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `POST /api/common/files/batch`                                                                                  |
| **Auth**     | Requires `UPLOAD_FILE` permission                                                                                |
| **Params**   | `files` (required)  Emultipart file array, `associateType` (optional), `associateId` (optional)                |
| **Content-Type** | `multipart/form-data`                                                                                       |

Uploads multiple files in a single request. Each file is processed independently with the same rules as single upload. Total number of files must not exceed `app.upload.max-files-per-request`.

Returns: Array of `id`, `storedName`, `originalFilename`, `datePath`, `dateUrl`, `size`, `contentType`, `md5`.

---

### Download File

| Item          | Detail                                           |
| ------------- | ------------------------------------------------- |
| **Endpoint**  | `GET /api/common/files/{dateUrl}/{storedName}`   |
| **Auth**      | Requires `DOWNLOAD_FILE` permission                  |
| **Response**  | Binary file content with `Content-Disposition: attachment` |

Downloads a file by its date URL and stored name. The server verifies MD5 integrity before serving: if the file on disk does not match the stored MD5 hash, a `FILE_MD5_MISMATCH` error is returned. The response includes the `X-File-MD5` header containing the stored MD5 hash, allowing client-side integrity verification.

---

### Verify File Integrity

| Item         | Detail                                                    |
| ------------ | --------------------------------------------------------- |
| **Endpoint** | `GET /api/common/files/{dateUrl}/{storedName}/verify`     |
| **Auth**     | Requires `DOWNLOAD_FILE` permission                           |

Verifies that the file on disk matches the stored MD5 hash in the database. Recomputes the MD5 of the physical file and compares it with the database record.

Returns: `{ "match": true/false, "storedMd5": "..." }`

---

### Delete File

| Item         | Detail                                           |
| ------------ | ------------------------------------------------- |
| **Endpoint** | `DELETE /api/common/files/{dateUrl}/{storedName}` |
| **Auth**     | Requires `REMOVE_FILE` permission                |

Deletes a file from disk and logically deletes its database record. Physical file is removed from the filesystem, and the database record is soft-deleted (`deleted` flag set to 1) via `@TableLogic`.

---

## Log System

### Get Access Log Page

| Item         | Detail                                                                                                                                      |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/logs/access/page`                                                                                                                 |
| **Auth**     | Requires `VIEW_LOG` permission                                                                                                              |
| **Params**   | `page`, `size`, `traceId` (exact match), `username` (partial match), `method`, `path` (partial match), `statusCode`, `startTime`, `endTime` |

Queries access logs with pagination support. Results ordered by creation time descending. Each item includes: `id`, `traceId`, `username`, `method`, `path`, `statusCode`, `duration`, `clientIp`, `createdAt`.

---

### Get Access Log Detail

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `GET /api/logs/access/{id}`    |
| **Auth**     | Requires `VIEW_LOG` permission |

Retrieves complete access log information by ID. Returns: `id`, `traceId`, `username`, `method`, `path`, `queryString`, `requestBody`, `responseBody`, `statusCode`, `duration`, `clientIp`, `userAgent`, `createdAt`. Throws 404 if not found.

---

### Get Access Log by TraceId

| Item         | Detail                                 |
| ------------ | -------------------------------------- |
| **Endpoint** | `GET /api/logs/access/trace/{traceId}` |
| **Auth**     | Requires `VIEW_LOG` permission         |

Retrieves access log by trace ID. Useful for debugging request chains. Throws 404 if not found.

---

### Export Access Logs

| Item         | Detail                           |
| ------------ | -------------------------------- |
| **Endpoint** | `GET /api/logs/access/export`    |
| **Auth**     | Requires `EXPORT_LOG` permission |

Exports access logs matching the query conditions as a CSV file. Limited to 10,000 records to avoid memory issues. Returns `text/csv` content type with UTF-8 BOM for Excel compatibility.

---

### Get Error Log Page

| Item         | Detail                                                                                                                            |
| ------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/logs/error/page`                                                                                                        |
| **Auth**     | Requires `VIEW_LOG` permission                                                                                                    |
| **Params**   | `page`, `size`, `traceId` (exact match), `keyword` (multi-field fuzzy search), `username` (partial match), `startTime`, `endTime` |

Queries error logs with pagination support. Results ordered by creation time descending. Each item includes: `id`, `traceId`, `errorType`, `message`, `username`, `createdAt`. The `keyword` parameter searches across: `exception_file`, `exception_class`, `exception_method`, `exception_message`, `root_cause_message`, `stack_trace` (OR condition).

---

### Get Error Log Detail

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `GET /api/logs/error/{id}`     |
| **Auth**     | Requires `VIEW_LOG` permission |

Retrieves complete error log information by ID. Returns: `id`, `traceId`, `errorType`, `message`, `stackTrace`, `username`, `requestMethod`, `requestPath`, `requestBody`, `createdAt`. Throws 404 if not found.

---

### Get Error Log by TraceId

| Item         | Detail                                |
| ------------ | ------------------------------------- |
| **Endpoint** | `GET /api/logs/error/trace/{traceId}` |
| **Auth**     | Requires `VIEW_LOG` permission        |

Retrieves error log by trace ID. Returns null if no error occurred during the request.

---

### Get Operation Log Page

| Item         | Detail                                                                                                                                                              |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/logs/operation/page`                                                                                                                                      |
| **Auth**     | Requires `VIEW_LOG` permission                                                                                                                                      |
| **Params**   | `page`, `size`, `traceId` (exact match), `operationType`, `module` (partial match), `subModule` (partial match), `username` (partial match), `startTime`, `endTime` |

Queries operation logs with pagination support. Results ordered by creation time descending. Each item includes: `id`, `traceId`, `module`, `subModule`, `operationType`, `description`, `duration`, `username`, `createdAt`.

---

### Get Operation Log Detail

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `GET /api/logs/operation/{id}` |
| **Auth**     | Requires `VIEW_LOG` permission |

Retrieves complete operation log information by ID. Returns: `id`, `traceId`, `module`, `subModule`, `operationType`, `description`, `duration`, `requestMethod`, `requestUrl`, `clientIp`, `username`, `createdAt`. Throws 404 if not found.

---

### Get Operation Logs by TraceId

| Item         | Detail                                    |
| ------------ | ----------------------------------------- |
| **Endpoint** | `GET /api/logs/operation/trace/{traceId}` |
| **Auth**     | Requires `VIEW_LOG` permission            |

Retrieves all operation logs for a specific trace ID. Results ordered by creation time ascending (chronological order). Useful for viewing the operation sequence within a single request.

---

### Get Module List

| Item         | Detail                            |
| ------------ | --------------------------------- |
| **Endpoint** | `GET /api/logs/operation/modules` |
| **Auth**     | Requires `VIEW_LOG` permission    |

Returns distinct module names for filtering dropdown. Only includes modules that have at least one operation log entry.

---

### Get Trace Detail

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `GET /api/logs/trace/{traceId}` |
| **Auth**     | Requires `VIEW_LOG` permission  |

Retrieves complete trace information by trace ID. Combines:

- **Access Log**: The original request details
- **Error Log**: Error information if the request failed (null otherwise)
- **Operation Logs**: List of all operations performed during this request

This endpoint provides a complete view of a request's lifecycle for debugging and auditing purposes.

---

---

# API 参老E��档

## 认证E
### 登彁E
| 项目         | 详惁E                  |
| ------------ | ---------------------- |
| **接口地址** | `POST /api/auth/login` |
| **杁E��**     | 公开�E�无需认证E��E      |

通迁ESpring Security 皁EAuthenticationManager 验证用户凭证。认证�E功后生�E JWT 访问令牌（在响应体中返回�E�和刷新令牌（通迁EHttpOnly Cookie 设置�E�。包含暴力破解防护�E�按账户咁EIP 两个维度记录登录失败�E�趁E��E�E值后锁定账户�E�非白名单 IP 自动封禁。在 Redis 中注册活跁E��话（若已有会话�E踢出旧会话）、E
---

### 刷新令牁E
| 项目         | 详惁E                    |
| ------------ | ------------------------ |
| **接口地址** | `POST /api/auth/refresh` |
| **杁E��**     | CSRF 保护�E�无需认证E     |

实现令牌轮换机制。仁EHttpOnly Cookie 中读取刷新令牌，验证�E有效性�E�类型、迁E��时间、撤销状态E��，撤销旧皁E��新令牌，重新加载用户信息以验证用户仍夁E��活跁E��态E��生成新皁E��牌对�E�并在 Redis 中更新活跁E��话、E
---

### 登出

| 项目         | 详惁E                   |
| ------------ | ----------------------- |
| **接口地址** | `POST /api/auth/logout` |
| **杁E��**     | 需要认证E+ CSRF 保护    |

通迁E��EJTI 添加到 Redis 黑名单来撤销访问令牌（来自 Authorization 头�E�和刷新令牌（来自 Cookie�E�。渁E��刷新令牁ECookie�E�仁ERedis 移除活跁E��话记录，并渁E�� UserDetails 缓存、E
---

## 角色管琁E
### 获取角色详惁E
| 项目         | 详惁E                 |
| ------------ | --------------------- |
| **接口地址** | `GET /api/roles/{id}` |
| **杁E��**     | 需要E`VIEW_ROLE` 杁E�� |

根据 ID 从数据库中获取单个角色。返回角色详惁E��包括 ID、名称、代码、描述和版本号。未找到时抛�E 404 异常、E
---

### 获取角色列表

| 项目         | 详惁E                                                                     |
| ------------ | ------------------------------------------------------------------------- |
| **接口地址** | `GET /api/roles`                                                          |
| **杁E��**     | 需要E`VIEW_ROLE` 杁E��                                                     |
| **参数**     | `roleName`�E�模糊匹配）、`roleCode`�E�前缀匹配）、`description`�E�模糊匹配！E|

根据可选条件查询角色�E�名称模糊匹配、代码前缀匹配、描述模糊匹配）。返回角色列表�E�包含吁E��色皁E��接父角色 ID 和版本号�E�用于删除时皁E��观锁�E�，通迁E��次批量查询解析以避允EN+1 问题。结果按更新时间降序排列、E
---

### 创建角色

| 项目         | 详惁E                |
| ------------ | -------------------- |
| **接口地址** | `POST /api/roles`    |
| **杁E��**     | 需要E`CREATE_ROLE` 杁E�� |

使用提供的名称、描述和可选的父角色 ID 创建新角色。角色代码不是忁E��皁E- 宁E��E��于基础角色。�E定义角色通迁E��承父角色获得权限。返回创建皁E��色 ID、E
**请求体！E*

- `roleName`�E�忁E���E�：角色名称�E�最长 64 字符
- `description`�E�可选）：角色描述�E�最长 255 字符
- `parentRoleIds`�E�可选）：父角色 ID 列表�E�用于继承关系

---

### 更新角色

| 项目         | 详惁E                 |
| ------------ | --------------------- |
| **接口地址** | `PUT /api/roles`      |
| **杁E��**     | 需要E`CREATE_ROLE` 杁E�� |

更新现有角色皁E��称、描述和继承关系。通迁E��本字段使用乐观锁确保并发安�E。如果角色被其他事务修改则抛出异常、E
**请求体！E*

- `id`�E�忁E���E�：角色 ID
- `roleName`�E�忁E���E�：角色名称�E�最长 64 字符
- `description`�E�可选）：角色描述�E�最长 255 字符
- `parentRoleIds`�E�可选）：父角色 ID 列表�E�用于继承关系�E�替换现有�E系�E�E- `version`�E�忁E���E�：版本号�E�用于乐观锁

---

### 删除角色

| 项目         | 详惁E                |
| ------------ | -------------------- |
| **接口地址** | `DELETE /api/roles`  |
| **杁E��**     | 需要E`DELETE_ROLE` 杁E�� |

验证角色未被任何用户使用且未被其他角色继承后删除角色。使用乐观锁。删除后渁E��该角色作为子角色皁E��有继承关系、E
**请求体！E*

- `id`�E�忁E���E�：角色 ID
- `version`�E�忁E���E�：版本号�E�用于乐观锁

---

## 用户管琁E
### 获取用户详惁E
| 项目         | 详惁E                 |
| ------------ | --------------------- |
| **接口地址** | `GET /api/users/{id}` |
| **杁E��**     | 需要E`VIEW_USER` 杁E�� |

根据 ID 获取单个用户。返回用户详惁E��包括 ID、用户名、状态、版本号�E�以及直接刁E�E皁E��色 ID 列表�E�EroleIds`�E�不为 null�E�用户无角色时为空数绁E��。未找到时抛�E 404 异常、E
---

### 获取用户列表

| 项目         | 详惁E                                        |
| ------------ | -------------------------------------------- |
| **接口地址** | `GET /api/users`                             |
| **杁E��**     | 需要E`VIEW_USER` 杁E��                        |
| **参数**     | `username`�E�模糊匹配）、`status`�E�精确匹配！E|

根据可选条件查询用户�E�用户名模糊匹配、状态精确匹配）。返回按更新时间降序排列的列表�E�每项匁E��版本号�E�用于删除/更新时皁E��观锁�E�、E
---

### 获取用户刁E��

| 项目         | 详惁E                                                        |
| ------------ | ------------------------------------------------------------ |
| **接口地址** | `GET /api/users/page`                                        |
| **杁E��**     | 需要E`VIEW_USER` 杁E��                                        |
| **参数**     | `page`、`size`、`username`�E�模糊匹配）、`status`�E�精确匹配！E|

支持�E页查询用户。接受页码、E��大小和可选迁E��条件。返回带有总数皁E�E页结果�E�每项匁E��版本号�E�用于删除/更新时皁E��观锁�E�、E
---

### 创建用户

| 项目         | 详惁E                |
| ------------ | -------------------- |
| **接口地址** | `POST /api/users`    |
| **杁E��**     | 需要E`CREATE_USER` 杁E�� |

创建新用户�E�验证用户名唯一性。寁E��E��迁EPasswordEncoder 加寁E��存储。支持E��迁EroleIds 同时刁E�E角色。返回创建皁E��户 ID、E
**请求体！E*

- `username`�E�忁E���E�：用户名，仁E�E许字母、数字和下�E线�E�E^[a-zA-Z0-9_]+$`�E�，最长 64 字符
- `password`�E�忁E���E�：寁E��E��E E28 字符
- `status`�E�忁E���E�：用户状态E��E0` 禁用 / `1` 启用�E�，忁E��为合法的 `UserStatusType` 值
- `roleIds`�E�可选）：要�E配的角色 ID 列表

---

### 更新用户

| 项目         | 详惁E                 |
| ------------ | --------------------- |
| **接口地址** | `PUT /api/users`      |
| **杁E��**     | 需要E`CREATE_USER` 杁E�� |

更新用户信息。用户名不可修改。如果提供寁E���E加寁E��储�E�否则保持不变。通迁E��本字段使用乐观锁。当传入 `roleIds` 时�E�按列表同步用户皁E��色刁E�E�E�新增缺失皁E��移除多余的�E�；未传入�E�Eull�E�时保持现有角色不变。更新后渁E�� UserDetails 缓存、E
**请求体！E*

- `id`�E�忁E���E�：用户 ID
- `username`�E�可选）：用户名（只读�E�需与当前值一致�E�，仁E�E许字母、数字和下�E线�E�最长 64 字符
- `password`�E�可选）：新寁E��E��E E28 字符�E�未传则保持不变
- `status`�E�可选）：用户状态E��E0` 禁用 / `1` 启用�E�E- `roleIds`�E�可选）：目栁E��色 ID 列表�E�未传保持现有角色�E�传 `[]` 封E��除所有角色
- `version`�E�忁E���E�：版本号�E�用于乐观锁

---

### 删除用户

| 项目         | 详惁E                |
| ------------ | -------------------- |
| **接口地址** | `DELETE /api/users`  |
| **杁E��**     | 需要E`DELETE_USER` 杁E�� |

使用乐观锁删除用户。删除后渁E�� UserDetails 缓存以确保一致性、E
**请求体！E*

- `id`�E�忁E���E�：用户 ID
- `version`�E�忁E���E�：版本号�E�用于乐观锁

---

### 生�E用户列表报表

| 项目         | 详惁E                                                   |
| ------------ | ------------------------------------------------------- |
| **接口地址** | `GET /api/users/report`                                 |
| **杁E��**     | 需要E`EXPORT_USER` 杁E��                               |
| **参数**     | `username`�E�模糊匹配）、`groupBy`�E�可选！E              |

根据用户名筛选条件生�E用户列表 PDF 报表。`groupBy` 参数控制报表中用户皁E�E绁E��式！E
- **null 或空**  E无刁E��E��所有匹配用户以单一表格列�E、E- **`role`**  E按用户直接刁E�E皁E��色刁E��E��拥有多个角色皁E��户会�E现在每个角色刁E��E���E�每个刁E��E��新页开始、E- **`baseRole`**  E按基础角色�E�继承链中皁E���E角色�E��E绁E��对于每个用户�E�收雁E�E直接角色皁E��有祖�E角色�E�用户会�E现在每个基础角色刁E��E���E�每个刁E��E��新页开始。如果直接角色没有祖�E角色�E��E用户出现在该直接角色皁E�E绁E��、E
返回 PDF 斁E���E�`Content-Type` 为 `application/pdf`�E�附带 `Content-Disposition: attachment` 头。报表栁E��根据客户端 `Accept-Language` 请求头自动本地化。导出记录数上限由配置控制�E�Eapp.report.max-export-records`�E�默认 10,000�E�、E
---

## 公共接口

### 获取枚举列表

| 项目         | 详惁E                                       |
| ------------ | ------------------------------------------- |
| **接口地址** | `GET /api/common/enums`                     |
| **杁E��**     | 公开�E�无需认证E��E                           |
| **参数**     | `type`  E支持值�E�`user-status`、`role-code`、`module`、`sub-module` |

根据类型返回枚举数据列表。支持E`user-status`�E�用户状态E��项�E�、`role-code`�E�角色代码E��项�E�、`module`�E�模块选项�E�和 `sub-module`�E�子模块选项�E�。每项匁E�� `code` 咁E`name` 字段、E
---

### 获取下拉选项

| 项目         | 详惁E                                                                  |
| ------------ | ---------------------------------------------------------------------- |
| **接口地址** | `GET /api/common/select-options`                                       |
| **权限**     | 控制器类级别 `@PreAuthorize`，所有类型都需要 `LIST_OPTIONS` 权限        |
| **参数**     | `type` — 通过 `SelectOptionProvider` 实现动态注册（如 `role`、`user`）|

返回前端下拉/选择组件的选项列表。Provider 实现通过 Spring DI 自动发现。添加新类型只需创建新的 `SelectOptionProvider` Bean，无需修改控制器。每个选项包含 `value`、`label` 和 `description`。
所有 `SelectOptionProvider` 类型都需要 `LIST_OPTIONS` 权限，通过在 `SelectOptionController` 类级别添加 `@PreAuthorize` 注解统一控制。
---

### 上传斁E��

| 项目           | 详惁E                                                                                                   |
| -------------- | ------------------------------------------------------------------------------------------------------- |
| **接口地址**   | `POST /api/common/files`                                                                               |
| **杁E��**       | 需要E`UPLOAD_FILE` 杁E��                                                                                  |
| **参数**       | `file`�E�忁E���E� E上传斁E���E�`associateType`�E�可选） E关联类型，`associateId`�E�可选） E关联业务实佁EID |
| **Content-Type** | `multipart/form-data`                                                                                |

上传单个斁E��到服务器。文件存储在按日期绁E��E��子目录！Eyyyy/MM/dd`�E�下，使用 UUID 斁E��名防止冲突和路征E��厁E��击。上传迁E��中通迁E`DigestInputStream` 计箁EMD5 哈希并存储到数据库，用于完整性校验。文件类型根据扩展名自动检测！E=图牁E��E=斁E���E�E=压缩匁E��E=其他）。扩展名忁E��在 `app.upload.allowed-extensions` 配置皁E�E许列表中。文件大小不得趁E��E`app.upload.max-file-size-mb`、E
返回�E�`id`、`storedName`、`originalFilename`、`datePath`、`dateUrl`、`size`、`contentType`、`md5`、E
---

### 批量上传斁E��

| 项目           | 详惁E                                                                                             |
| -------------- | ------------------------------------------------------------------------------------------------- |
| **接口地址**   | `POST /api/common/files/batch`                                                                   |
| **杁E��**       | 需要E`UPLOAD_FILE` 杁E��                                                                            |
| **参数**       | `files`�E�忁E���E� E上传斁E��数绁E��`associateType`�E�可选），`associateId`�E�可选！E                  |
| **Content-Type** | `multipart/form-data`                                                                          |

批量上传多个斁E��。每个斁E��独立夁E���E�见E�E与单斁E��上传相同。文件总数不得趁E��E`app.upload.max-files-per-request`、E
返回�E�`id`、`storedName`、`originalFilename`、`datePath`、`dateUrl`、`size`、`contentType`、`md5` 皁E��绁E��E
---

### 下载斁E��

| 项目           | 详惁E                                               |
| -------------- | --------------------------------------------------- |
| **接口地址**   | `GET /api/common/files/{dateUrl}/{storedName}`      |
| **杁E��**       | 需要E`DOWNLOAD_FILE` 杁E��                               |
| **响庁E*       | 二进制斁E��冁E���E�包含 `Content-Disposition: attachment` |

通迁E��朁EURL 和存储斁E��名下载斁E��。服务器在返回斁E��前验证EMD5 完整性�E�如果磁盘斁E��与存储皁EMD5 哈希不匹配，返回 `FILE_MD5_MISMATCH` 错误。响应包含 `X-File-MD5` 头�E�用于客户端完整性校验、E
---

### 验证文件完整性

| 项目           | 详惁E                                                     |
| -------------- | --------------------------------------------------------- |
| **接口地址**   | `GET /api/common/files/{dateUrl}/{storedName}/verify`     |
| **杁E��**       | 需要E`DOWNLOAD_FILE` 杁E��                                      |

验证磁盘斁E��与数据库存储皁EMD5 哈希是否匹配。重新计算物琁E��件皁EMD5 并与数据库记录比辁E��E
返回�E�`{ "match": true/false, "storedMd5": "..." }`

---

### 删除斁E��

| 项目           | 详惁E                                          |
| -------------- | ---------------------------------------------- |
| **接口地址**   | `DELETE /api/common/files/{dateUrl}/{storedName}` |
| **杁E��**       | 需要E`REMOVE_FILE` 杁E��                        |

从磁盘删除斁E��并逻辑删除数据库记录。物琁E��件从文件系统中移除�E�数据库记录通迁E`@TableLogic` 软删除�E�Edeleted` 栁E��设为 1�E�、E
---

## 日志系绁E
### 获取访问日志�E页

| 项目         | 详惁E                                                                                                                             |
| ------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| **接口地址** | `GET /api/logs/access/page`                                                                                                       |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��                                                                                                              |
| **参数**     | `page`、`size`、`traceId`�E�精确匹配）、`username`�E�模糊匹配）、`method`、`path`�E�模糊匹配）、`statusCode`、`startTime`、`endTime` |

刁E��查询访问日志。结果按�E建时间降序排列。每项匁E���E�`id`、`traceId`、`username`、`method`、`path`、`statusCode`、`duration`、`clientIp`、`createdAt`、E
---

### 获取访问日志详惁E
| 项目         | 详惁E                       |
| ------------ | --------------------------- |
| **接口地址** | `GET /api/logs/access/{id}` |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��        |

根据 ID 获取访问日志完整信息。返回�E�`id`、`traceId`、`username`、`method`、`path`、`queryString`、`requestBody`、`responseBody`、`statusCode`、`duration`、`clientIp`、`userAgent`、`createdAt`。未找到时抛�E 404 异常、E
---

### 持ETraceId 获取访问日忁E
| 项目         | 详惁E                                  |
| ------------ | -------------------------------------- |
| **接口地址** | `GET /api/logs/access/trace/{traceId}` |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��                   |

根据链路追踪 ID 获取访问日志。用于谁E��请求链路。未找到时抛�E 404 异常、E
---

### 导出访问日忁E
| 项目         | 详惁E                         |
| ------------ | ----------------------------- |
| **接口地址** | `GET /api/logs/access/export` |
| **杁E��**     | 需要E`EXPORT_LOG` 杁E��        |

导出符合条件皁E��问日志为 CSV 斁E��。限制最夁E10,000 条记录以避免�E存问题。返回 `text/csv` 冁E��类型，包含 UTF-8 BOM 以兼容 Excel、E
---

### 获取错误日志�E页

| 项目         | 详惁E                                                                                                              |
| ------------ | ------------------------------------------------------------------------------------------------------------------ |
| **接口地址** | `GET /api/logs/error/page`                                                                                         |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��                                                                                               |
| **参数**     | `page`、`size`、`traceId`�E�精确匹配）、`keyword`�E�多字段模糊搜索�E�、`username`�E�模糊匹配）、`startTime`、`endTime` |

刁E��查询错误日志。结果按�E建时间降序排列。每项匁E���E�`id`、`traceId`、`errorType`、`message`、`username`、`createdAt`。`keyword` 参数支持在以下字段中模糊搜索�E�`exception_file`、`exception_class`、`exception_method`、`exception_message`、`root_cause_message`、`stack_trace`�E�ER 条件�E�满足任一字段即可�E�、E
---

### 获取错误日志详惁E
| 项目         | 详惁E                      |
| ------------ | -------------------------- |
| **接口地址** | `GET /api/logs/error/{id}` |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��       |

根据 ID 获取错误日志完整信息。返回�E�`id`、`traceId`、`errorType`、`message`、`stackTrace`、`username`、`requestMethod`、`requestPath`、`requestBody`、`createdAt`。未找到时抛�E 404 异常、E
---

### 持ETraceId 获取错误日忁E
| 项目         | 详惁E                                 |
| ------------ | ------------------------------------- |
| **接口地址** | `GET /api/logs/error/trace/{traceId}` |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��                  |

根据链路追踪 ID 获取错误日志。如果请求未出错�E返回 null、E
---

### 获取操作日志�E页

| 项目         | 详惁E                                                                                                                                                 |
| ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| **接口地址** | `GET /api/logs/operation/page`                                                                                                                        |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��                                                                                                                                  |
| **参数**     | `page`、`size`、`traceId`�E�精确匹配）、`operationType`、`module`�E�模糊匹配）、`subModule`�E�模糊匹配）、`username`�E�模糊匹配）、`startTime`、`endTime` |

刁E��查询操作日志。结果按�E建时间降序排列。每项匁E���E�`id`、`traceId`、`module`、`subModule`、`operationType`、`description`、`duration`、`username`、`createdAt`、E
---

### 获取操作日志详惁E
| 项目         | 详惁E                          |
| ------------ | ------------------------------ |
| **接口地址** | `GET /api/logs/operation/{id}` |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��           |

根据 ID 获取操作日志完整信息。返回�E�`id`、`traceId`、`module`、`subModule`、`operationType`、`description`、`duration`、`requestMethod`、`requestUrl`、`clientIp`、`username`、`createdAt`。未找到时抛�E 404 异常、E
---

### 持ETraceId 获取操作日志�E表

| 项目         | 详惁E                                     |
| ------------ | ----------------------------------------- |
| **接口地址** | `GET /api/logs/operation/trace/{traceId}` |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��                      |

根据链路追踪 ID 获取所有操作日志。结果按�E建时间十E��排列（时间顺序）。用于查看单次请求�E皁E��作序�E、E
---

### 获取模块�E表

| 项目         | 详惁E                             |
| ------------ | --------------------------------- |
| **接口地址** | `GET /api/logs/operation/modules` |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��              |

返回去重的模块名称列表�E�用于筛选下拉桁E��仁E��含有操作日志记录的模块、E
---

### 获取链路追踪详惁E
| 项目         | 详惁E                           |
| ------------ | ------------------------------- |
| **接口地址** | `GET /api/logs/trace/{traceId}` |
| **杁E��**     | 需要E`VIEW_LOG` 杁E��            |

根据链路追踪 ID 获取完整皁E��路信息。绁E��返回�E�E
- **访问日忁E*�E�原始请求详惁E- **错误日忁E*�E�请求失败时皁E��误信息�E��E功�E为 null�E�E- **操作日志�E表**�E�该请求期间执行的所有操佁E
此接口提供请求生命周期的完整见E���E�用于谁E��和审计、E
---

---

# API リファレンス

## 認証

### ログイン

| 頁E��               | 詳細                   |
| ------------------ | ---------------------- |
| **エンド�EインチE* | `POST /api/auth/login` |
| **認可**           | 公開（認証不要E��E      |

Spring Security の AuthenticationManager を介してユーザー認証惁E��を検証します。認証成功時に JWT アクセスト�Eクン�E�レスポンスボディで返却�E�とリフレチE��ュト�Eクン�E�EttpOnly Cookie で設定）を生�Eします。ブルートフォース保護機�Eを含み、アカウントおよ�E IP ごとにログイン失敗を記録し、E��値趁E��後にアカウントをロチE��、�Eワイトリスト外�E IP を�E動ブロチE��します。Redis にアクチE��ブセチE��ョンを登録します（既存セチE��ョンがある場合�E強制退出�E�、E
---

### ト�EクンリフレチE��ュ

| 頁E��               | 詳細                     |
| ------------------ | ------------------------ |
| **エンド�EインチE* | `POST /api/auth/refresh` |
| **認可**           | CSRF 保護、認証不要E     |

ト�EクンローチE�Eションを実裁E��ます、EttpOnly Cookie からリフレチE��ュト�Eクンを読み取り、有効性�E�タイプ、有効期限、失効状態）を検証し、古ぁE��フレチE��ュト�Eクンを失効させ、ユーザー惁E��を�E読み込みしてユーザーがアクチE��ブであることを確認し、新しいト�Eクンペアを生成し、Redis のアクチE��ブセチE��ョンを更新します、E
---

### ログアウチE
| 頁E��               | 詳細                    |
| ------------------ | ----------------------- |
| **エンド�EインチE* | `POST /api/auth/logout` |
| **認可**           | 認証忁E��E+ CSRF 保護    |

Authorization ヘッダーのアクセスト�Eクンと Cookie のリフレチE��ュト�Eクンの JTI めERedis ブラチE��リストに追加して失効させます。リフレチE��ュト�Eクン Cookie をクリアし、Redis からアクチE��ブセチE��ョンレコードを削除し、UserDetails キャチE��ュを無効化します、E
---

## ロール管琁E
### ロール詳細取征E
| 頁E��               | 詳細                   |
| ------------------ | ---------------------- |
| **エンド�EインチE* | `GET /api/roles/{id}`  |
| **認可**           | `VIEW_ROLE` 権限が忁E��E|

ID でチE�Eタベ�Eスから単一のロールを取得します、ED、名前、コード、説明、バージョンを含むロール詳細を返します。見つからなぁE��合�E 404 例外をスローします、E
---

### ロール一覧取征E
| 頁E��               | 詳細                                                                      |
| ------------------ | ------------------------------------------------------------------------- |
| **エンド�EインチE* | `GET /api/roles`                                                          |
| **認可**           | `VIEW_ROLE` 権限が忁E��E                                                   |
| **パラメータ**     | `roleName`�E�部刁E��致�E�、`roleCode`�E�前方一致�E�、`description`�E�部刁E��致�E�E|

オプション条件�E�名前部刁E��致、コード前方一致、説明部刁E��致�E�でロールを検索します。各ロールの直接親ロール ID とバ�Eジョン�E�削除時�E楽観皁E��チE��用�E�を含むリストを返し、N+1 問題を回避するため単一バッチクエリで解決します。結果は更新日時�E降頁E��並べられます、E
---

### ロール作�E

| 頁E��               | 詳細                  |
| ------------------ | --------------------- |
| **エンド�EインチE* | `POST /api/roles`     |
| **認可**           | `CREATE_ROLE` 権限が忁E��E|

提供された名前、説明、およ�Eオプションの親ロール ID で新しいロールを作�Eします。ロールコード�E忁E��ではありません - ベ�Eスロールのみで使用されます。カスタムロールは親ロールから権限を継承します。作�Eされたロール ID を返します、E
**リクエスト�EチE���E�E*

- `roleName`�E�忁E��）：ロール名、最大 64 斁E��E- `description`�E�オプション�E�：ロールの説明、最大 255 斁E��E- `parentRoleIds`�E�オプション�E�：継承用の親ロール ID リスチE
---

### ロール更新

| 頁E��               | 詳細                   |
| ------------------ | ---------------------- |
| **エンド�EインチE* | `PUT /api/roles`       |
| **認可**           | `CREATE_ROLE` 権限が忁E��E|

既存ロールの名前、説明、およ�E継承関係を更新します。バージョンフィールドによる楽観皁E��チE��で並行�E琁E�E安�E性を確保します。他�Eトランザクションによって変更された場合�E例外をスローします、E
**リクエスト�EチE���E�E*

- `id`�E�忁E��）：ロール ID
- `roleName`�E�忁E��）：ロール名、最大 64 斁E��E- `description`�E�オプション�E�：ロールの説明、最大 255 斁E��E- `parentRoleIds`�E�オプション�E�：継承用の親ロール ID リスト（既存�E関係を置換！E- `version`�E�忁E��）：楽観皁E��チE��用のバ�Eジョン

---

### ロール削除

| 頁E��               | 詳細                  |
| ------------------ | --------------------- |
| **エンド�EインチE* | `DELETE /api/roles`   |
| **認可**           | `DELETE_ROLE` 権限が忁E��E|

ロールがユーザーに割り当てられておらず、他�Eロールに継承されてぁE��ぁE��とを検証後に削除します。楽観皁E��チE��を使用します。削除後、このロールが子ロールである全ての継承関係をクリーンアチE�Eします、E
**リクエスト�EチE���E�E*

- `id`�E�忁E��）：ロール ID
- `version`�E�忁E��）：楽観皁E��チE��用のバ�Eジョン

---

## ユーザー管琁E
### ユーザー詳細取征E
| 頁E��               | 詳細                   |
| ------------------ | ---------------------- |
| **エンド�EインチE* | `GET /api/users/{id}`  |
| **認可**           | `VIEW_USER` 権限が忁E��E|

ID でユーザーを取得します、ED、ユーザー名、スチE�Eタス、バージョン、およ�E直接割り当てられたロール ID リスト！EroleIds`、null にならず、ロールがなぁE��合�E空配�E�E�を含むユーザー詳細を返します。見つからなぁE��合�E 404 例外をスローします、E
---

### ユーザー一覧取征E
| 頁E��               | 詳細                                         |
| ------------------ | -------------------------------------------- |
| **エンド�EインチE* | `GET /api/users`                             |
| **認可**           | `VIEW_USER` 権限が忁E��E                      |
| **パラメータ**     | `username`�E�部刁E��致�E�、`status`�E�完�E一致�E�E|

オプション条件�E�ユーザー名部刁E��致、スチE�Eタス完�E一致�E�でユーザーを検索します。各頁E��にバ�Eジョン�E�削除・更新時�E楽観皁E��チE��用�E�を含み、更新日時�E降頁E��リストを返します、E
---

### ユーザーペ�Eジング取征E
| 頁E��               | 詳細                                                         |
| ------------------ | ------------------------------------------------------------ |
| **エンド�EインチE* | `GET /api/users/page`                                        |
| **認可**           | `VIEW_USER` 権限が忁E��E                                      |
| **パラメータ**     | `page`、`size`、`username`�E�部刁E��致�E�、`status`�E�完�E一致�E�E|

ペ�Eジネ�Eション付きでユーザーを検索します。�Eージ番号、�Eージサイズ、オプションのフィルター条件を受け付けます。各頁E��にバ�Eジョン�E�削除・更新時�E楽観皁E��チE��用�E�を含み、総数を含むペ�Eジング結果を返します、E
---

### ユーザー作�E

| 頁E��               | 詳細                  |
| ------------------ | --------------------- |
| **エンド�EインチE* | `POST /api/users`     |
| **認可**           | `CREATE_USER` 権限が忁E��E|

ユーザー名�E一意性を検証して新規ユーザーを作�Eします。パスワード�E PasswordEncoder で暗号化して保存します。roleIds による同時ロール割り当てをサポ�Eトします。作�Eされたユーザー ID を返します、E
**リクエスト�EチE���E�E*

- `username`�E�忁E��）：ユーザー名、英数字とアンダースコアのみ�E�E^[a-zA-Z0-9_]+$`�E�、最大 64 斁E��E- `password`�E�忁E��）：パスワード、E�E�E28 斁E��E- `status`�E�忁E��）：ユーザースチE�Eタス�E�E0` 無効 / `1` 有効�E�、有効な `UserStatusType` 値である忁E��がありまぁE- `roleIds`�E�任意）：割り当てるロール ID リスチE
---

### ユーザー更新

| 頁E��               | 詳細                   |
| ------------------ | ---------------------- |
| **エンド�EインチE* | `PUT /api/users`       |
| **認可**           | `CREATE_USER` 権限が忁E��E|

ユーザー惁E��を更新します。ユーザー名�E変更できません。パスワードが提供された場合�E暗号化して保存し、そぁE��なければ変更しません。バージョンフィールドによる楽観皁E��チE��を使用します。`roleIds` が指定された場合、ユーザーのロール割り当てをそのリストに同期します（不足刁E��追加、余�Eを削除�E�。省略�E�Eull�E��E場合�E既存ロールをそのままとします。更新後に UserDetails キャチE��ュを無効化します、E
**リクエスト�EチE���E�E*

- `id`�E�忁E��）：ユーザー ID
- `username`�E�任意）：ユーザー名（読み取り専用、現在の値と同じである忁E��E��、英数字とアンダースコアのみ、最大 64 斁E��E- `password`�E�任意）：新しいパスワード、E�E�E28 斁E��。省略時�E変更されません
- `status`�E�任意）：ユーザースチE�Eタス�E�E0` 無効 / `1` 有効�E�E- `roleIds`�E�任意）：目標ロール ID リスト。省略時�E現在のロールを維持、`[]` を渡すと全て削除
- `version`�E�忁E��）：楽観皁E��チE��用のバ�Eジョン

---

### ユーザー削除

| 頁E��               | 詳細                  |
| ------------------ | --------------------- |
| **エンド�EインチE* | `DELETE /api/users`   |
| **認可**           | `DELETE_USER` 権限が忁E��E|

楽観皁E��チE��を使用してユーザーを削除します。削除後に UserDetails キャチE��ュを無効化して整合性を確保します、E
**リクエスト�EチE���E�E*

- `id`�E�忁E��）：ユーザー ID
- `version`�E�忁E��）：楽観皁E��チE��用のバ�Eジョン

---

### ユーザー一覧レポ�Eト生戁E
| 頁E��               | 詳細                                                      |
| ------------------ | --------------------------------------------------------- |
| **エンド�EインチE* | `GET /api/users/report`                                   |
| **認可**           | `EXPORT_USER` 権限が忁E��E                               |
| **パラメータ**     | `username`�E�部刁E��致�E�、`groupBy`�E�オプション�E�E          |

ユーザー名フィルタに一致するユーザーの PDF レポ�Eトを生�Eします。`groupBy` パラメータはレポ�Eト�Eのグループ化方法を制御します！E
- **null また�E空白**  Eグループ化なし。一致する全ユーザーを単一チE�Eブルで表示します、E- **`role`**  Eユーザーの直接割り当てロールでグループ化します。褁E��ロールを持つユーザーは吁E��ールグループに表示され、各グループ�E改ペ�Eジから開始します、E- **`baseRole`**  Eベ�Eスロール�E�継承チェーンの祖�Eロール�E�でグループ化します。各ユーザーの直接ロールの全祖�Eロールを収雁E��、ユーザーは吁E�Eースロールグループに表示されます。各グループ�E改ペ�Eジから開始します。直接ロールに祖�EがなぁE��合、ユーザーはそ�E直接ロールのグループに表示されます、E
`Content-Type: application/pdf` および `Content-Disposition: attachment` ヘッダー付きの PDF ファイルを返します。レポ�Eトラベルはクライアント�E `Accept-Language` ヘッダーに基づぁE��自動的にローカライズされます。エクスポ�Eトレコード数の上限は設定で制御されます！Eapp.report.max-export-records`、デフォルチE10,000�E�、E
---

## 共送E
### 列挙リスト取征E
| 頁E��               | 詳細                                            |
| ------------------ | ----------------------------------------------- |
| **エンド�EインチE* | `GET /api/common/enums`                         |
| **認可**           | 公開（認証不要E��E                               |
| **パラメータ**     | `type`  Eサポ�Eト値�E�`user-status`、`role-code`、`module`、`sub-module` |

タイプ別に列挙チE�Eタリストを返します。`user-status`�E�ユーザースチE�Eタスオプション�E�、`role-code`�E�ロールコードオプション�E�、`module`�E�モジュールオプション�E�、`sub-module`�E�サブモジュールオプション�E�をサポ�Eトします。各頁E��には `code` と `name` フィールドが含まれます、E
---

### セレクトオプション取征E
| 頁E��               | 詳細                                                                     |
| ------------------ | ------------------------------------------------------------------------ |
| **エンド�EインチE* | `GET /api/common/select-options`                                         |
| **認可**           | コントローラクラスレベルの `@PreAuthorize` により全タイプに `LIST_OPTIONS` が必要 |
| **パラメータ**     | `type` — `SelectOptionProvider` 実装により動的登録（例：`role`、`user`）|

フロントエンドのドロップダウン/セレクトコンポーネント用のオプションリストを返します。Provider 実装は Spring DI により自動検出されます。新しいタイプを追加するには、新しい `SelectOptionProvider` Bean を作成するだけで、コントローラーの変更は不要です。各オプションには `value`、`label`、`description` が含まれます。
すべての `SelectOptionProvider` タイプに `LIST_OPTIONS` 権限が必要です。`SelectOptionController` のクラスレベルで `@PreAuthorize` により適用されます。
---

### ファイルアチE�EローチE
| 頁E��               | 詳細                                                                                                  |
| ------------------ | ----------------------------------------------------------------------------------------------------- |
| **エンド�EインチE* | `POST /api/common/files`                                                                              |
| **認可**           | `UPLOAD_FILE` 権限が忁E��E                                                                             |
| **パラメータ**     | `file`�E�忁E��） Eマルチパートファイル、`associateType`�E�任意） E関連タイプ、`associateId`�E�任意） E関連エンチE��チE�� ID |
| **Content-Type**   | `multipart/form-data`                                                                                |

単一ファイルをサーバ�EにアチE�Eロードします。ファイルは日付�Eースのサブディレクトリ�E�Eyyyy/MM/dd`�E�に UUID ベ�Eスのファイル名で保存され、競合とパストラバ�Eサル攻撁E��防止します。アチE�Eロード中に `DigestInputStream` で MD5 ハッシュを計算し、整合性検証のためチE�Eタベ�Eスに保存します。ファイルタイプ�E拡張子から�E動検�Eされます！E=画像、E=ドキュメント、E=アーカイブ、E=そ�E他）。拡張子�E `app.upload.allowed-extensions` で設定された許可リストに含まれてぁE��忁E��があります。ファイルサイズは `app.upload.max-file-size-mb` を趁E��ることはできません、E
戻り値�E�`id`、`storedName`、`originalFilename`、`datePath`、`dateUrl`、`size`、`contentType`、`md5`、E
---

### ファイル一括アチE�EローチE
| 頁E��               | 詳細                                                                                              |
| ------------------ | ------------------------------------------------------------------------------------------------- |
| **エンド�EインチE* | `POST /api/common/files/batch`                                                                   |
| **認可**           | `UPLOAD_FILE` 権限が忁E��E                                                                         |
| **パラメータ**     | `files`�E�忁E��） Eマルチパートファイル配�E、`associateType`�E�任意）、`associateId`�E�任意！E      |
| **Content-Type**   | `multipart/form-data`                                                                            |

褁E��ファイルを一括アチE�Eロードします。各ファイルは単一アチE�Eロードと同じルールで独立して処琁E��れます。ファイル総数は `app.upload.max-files-per-request` を趁E��ることはできません、E
戻り値�E�`id`、`storedName`、`originalFilename`、`datePath`、`dateUrl`、`size`、`contentType`、`md5` の配�E、E
---

### ファイルダウンローチE
| 頁E��               | 詳細                                                    |
| ------------------ | ------------------------------------------------------- |
| **エンド�EインチE* | `GET /api/common/files/{dateUrl}/{storedName}`           |
| **認可**           | `DOWNLOAD_FILE` 権限が忁E��E                                 |
| **レスポンス**     | `Content-Disposition: attachment` ヘッダー付きのバイナリファイル |

日仁EURL と保存名でファイルをダウンロードします。サーバ�Eは返却前に MD5 整合性を検証します。ディスク上�Eファイルが保存された MD5 ハッシュと一致しなぁE��合、`FILE_MD5_MISMATCH` エラーが返されます。レスポンスには `X-File-MD5` ヘッダーが含まれ、クライアント�Eで整合性検証に使用できます、E
---

### ファイル整合性検証

| 頁E��               | 詳細                                                          |
| ------------------ | ------------------------------------------------------------- |
| **エンド�EインチE* | `GET /api/common/files/{dateUrl}/{storedName}/verify`         |
| **認可**           | `DOWNLOAD_FILE` 権限が忁E��E                                       |

チE��スク上�Eファイルがデータベ�Eスに保存された MD5 ハッシュと一致するか検証します。物琁E��ァイルの MD5 を�E計算し、データベ�Eスレコードと比輁E��ます、E
戻り値�E�`{ "match": true/false, "storedMd5": "..." }`

---

### ファイル削除

| 頁E��               | 詳細                                                |
| ------------------ | --------------------------------------------------- |
| **エンド�EインチE* | `DELETE /api/common/files/{dateUrl}/{storedName}`   |
| **認可**           | `REMOVE_FILE` 権限が忁E��E                           |

チE��スクからファイルを削除し、データベ�Eスレコードを論理削除します。物琁E��ァイルはファイルシスチE��から削除され、データベ�Eスレコード�E `@TableLogic` により論理削除�E�Edeleted` フラグめE1 に設定）されます、E
---

## ログシスチE��

### アクセスログペ�Eジング取征E
| 頁E��               | 詳細                                                                                                                              |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| **エンド�EインチE* | `GET /api/logs/access/page`                                                                                                       |
| **認可**           | `VIEW_LOG` 権限が忁E��E                                                                                                            |
| **パラメータ**     | `page`、`size`、`traceId`�E�完�E一致�E�、`username`�E�部刁E��致�E�、`method`、`path`�E�部刁E��致�E�、`statusCode`、`startTime`、`endTime` |

ペ�Eジネ�Eション付きでアクセスログを検索します。結果は作�E日時�E降頁E��並べられます。各頁E��には `id`、`traceId`、`username`、`method`、`path`、`statusCode`、`duration`、`clientIp`、`createdAt` が含まれます、E
---

### アクセスログ詳細取征E
| 頁E��               | 詳細                        |
| ------------------ | --------------------------- |
| **エンド�EインチE* | `GET /api/logs/access/{id}` |
| **認可**           | `VIEW_LOG` 権限が忁E��E      |

ID でアクセスログの完�Eな惁E��を取得します。`id`、`traceId`、`username`、`method`、`path`、`queryString`、`requestBody`、`responseBody`、`statusCode`、`duration`、`clientIp`、`userAgent`、`createdAt` を返します。見つからなぁE��合�E 404 例外をスローします、E
---

### TraceId でアクセスログ取征E
| 頁E��               | 詳細                                   |
| ------------------ | -------------------------------------- |
| **エンド�EインチE* | `GET /api/logs/access/trace/{traceId}` |
| **認可**           | `VIEW_LOG` 権限が忁E��E                 |

トレース ID でアクセスログを取得します。リクエストチェーンのチE��チE��に使用します。見つからなぁE��合�E 404 例外をスローします、E
---

### アクセスログエクスポ�EチE
| 頁E��               | 詳細                          |
| ------------------ | ----------------------------- |
| **エンド�EインチE* | `GET /api/logs/access/export` |
| **認可**           | `EXPORT_LOG` 権限が忁E��E      |

条件に一致するアクセスログめECSV ファイルとしてエクスポ�Eトします。メモリ問題を避けるため最大 10,000 件に制限されます、Excel 互換性のため UTF-8 BOM 付きの `text/csv` コンチE��チE��イプを返します、E
---

### エラーログペ�Eジング取征E
| 頁E��               | 詳細                                                                                                                           |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| **エンド�EインチE* | `GET /api/logs/error/page`                                                                                                     |
| **認可**           | `VIEW_LOG` 権限が忁E��E                                                                                                         |
| **パラメータ**     | `page`、`size`、`traceId`�E�完�E一致�E�、`keyword`�E�褁E��フィールドあぁE��ぁE��索�E�、`username`�E�部刁E��致�E�、`startTime`、`endTime` |

ペ�Eジネ�Eション付きでエラーログを検索します。結果は作�E日時�E降頁E��並べられます。各頁E��には `id`、`traceId`、`errorType`、`message`、`username`、`createdAt` が含まれます。`keyword` パラメータは `exception_file`、`exception_class`、`exception_method`、`exception_message`、`root_cause_message`、`stack_trace` フィールドをあいまぁE��索します！ER 条件�E�、E
---

### エラーログ詳細取征E
| 頁E��               | 詳細                       |
| ------------------ | -------------------------- |
| **エンド�EインチE* | `GET /api/logs/error/{id}` |
| **認可**           | `VIEW_LOG` 権限が忁E��E     |

ID でエラーログの完�Eな惁E��を取得します。`id`、`traceId`、`errorType`、`message`、`stackTrace`、`username`、`requestMethod`、`requestPath`、`requestBody`、`createdAt` を返します。見つからなぁE��合�E 404 例外をスローします、E
---

### TraceId でエラーログ取征E
| 頁E��               | 詳細                                  |
| ------------------ | ------------------------------------- |
| **エンド�EインチE* | `GET /api/logs/error/trace/{traceId}` |
| **認可**           | `VIEW_LOG` 権限が忁E��E                |

トレース ID でエラーログを取得します。リクエストがエラーで終亁E��なかった場合�E null を返します、E
---

### 操作ログペ�Eジング取征E
| 頁E��               | 詳細                                                                                                                                                  |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| **エンド�EインチE* | `GET /api/logs/operation/page`                                                                                                                        |
| **認可**           | `VIEW_LOG` 権限が忁E��E                                                                                                                                |
| **パラメータ**     | `page`、`size`、`traceId`�E�完�E一致�E�、`operationType`、`module`�E�部刁E��致�E�、`subModule`�E�部刁E��致�E�、`username`�E�部刁E��致�E�、`startTime`、`endTime` |

ペ�Eジネ�Eション付きで操作ログを検索します。結果は作�E日時�E降頁E��並べられます。各頁E��には `id`、`traceId`、`module`、`subModule`、`operationType`、`description`、`duration`、`username`、`createdAt` が含まれます、E
---

### 操作ログ詳細取征E
| 頁E��               | 詳細                           |
| ------------------ | ------------------------------ |
| **エンド�EインチE* | `GET /api/logs/operation/{id}` |
| **認可**           | `VIEW_LOG` 権限が忁E��E         |

ID で操作ログの完�Eな惁E��を取得します。`id`、`traceId`、`module`、`subModule`、`operationType`、`description`、`duration`、`requestMethod`、`requestUrl`、`clientIp`、`username`、`createdAt` を返します。見つからなぁE��合�E 404 例外をスローします、E
---

### TraceId で操作ログリスト取征E
| 頁E��               | 詳細                                      |
| ------------------ | ----------------------------------------- |
| **エンド�EインチE* | `GET /api/logs/operation/trace/{traceId}` |
| **認可**           | `VIEW_LOG` 権限が忁E��E                    |

トレース ID ですべての操作ログを取得します。結果は作�E日時�E昁E��E��時系列頁E��で並べられます。単一リクエスト�Eの操作シーケンスを表示するために使用します、E
---

### モジュールリスト取征E
| 頁E��               | 詳細                              |
| ------------------ | --------------------------------- |
| **エンド�EインチE* | `GET /api/logs/operation/modules` |
| **認可**           | `VIEW_LOG` 権限が忁E��E            |

フィルタリング用ドロチE�Eダウンのための重褁E��除されたモジュール名リストを返します。操作ログレコードが存在するモジュールのみが含まれます、E
---

### トレース詳細取征E
| 頁E��               | 詳細                            |
| ------------------ | ------------------------------- |
| **エンド�EインチE* | `GET /api/logs/trace/{traceId}` |
| **認可**           | `VIEW_LOG` 権限が忁E��E          |

トレース ID で完�Eなトレース惁E��を取得します。以下を絁E��合わせて返します！E
- **アクセスログ**�E��Eのリクエスト詳細
- **エラーログ**�E�リクエスト失敗時のエラー惁E���E��E功時は null�E�E- **操作ログリスチE*�E�このリクエスト中に実行されたすべての操佁E
こ�Eエンド�Eイント�E、デバッグおよび監査目皁E��リクエスト�Eライフサイクルの完�Eなビューを提供します、E