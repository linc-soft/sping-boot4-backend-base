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
| **Auth**     | Requires `ROLE_VIEW` permission |

Retrieves a single role by ID from the database. Returns role details including ID, name, code, description, and version. Throws 404 if not found.

---

### Get Role List

| Item         | Detail                                                                               |
| ------------ | ------------------------------------------------------------------------------------ |
| **Endpoint** | `GET /api/roles`                                                                     |
| **Auth**     | Requires `ROLE_VIEW` permission                                                      |
| **Params**   | `roleName` (partial match), `roleCode` (prefix match), `description` (partial match) |

Queries roles by optional conditions (name partial match, code prefix match, description partial match). Returns a list of roles with their direct parent role IDs, resolved in a single batch query to avoid N+1 problems. Results ordered by update time descending.

---

### Create Role

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `POST /api/roles`              |
| **Auth**     | Requires `ROLE_NEW` permission |

Creates a new role with the provided name, description, and optional parent role IDs for inheritance. Role code is not required - it's only used for base roles. Custom roles inherit permissions from their parent roles. Returns the created role ID.

**Request Body:**

- `roleName` (required): Role name
- `description` (optional): Role description
- `parentRoleIds` (optional): List of parent role IDs for inheritance

---

### Update Role

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `PUT /api/roles`                |
| **Auth**     | Requires `ROLE_EDIT` permission |

Updates an existing role's name, description, and inheritance relationships. Uses optimistic locking via version field to ensure concurrency safety. Throws exception if the role was modified by another transaction.

**Request Body:**

- `id` (required): Role ID
- `roleName` (required): Role name
- `description` (optional): Role description
- `parentRoleIds` (optional): List of parent role IDs for inheritance (replaces existing relationships)
- `version` (required): Version for optimistic locking

---

### Delete Role

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `DELETE /api/roles`            |
| **Auth**     | Requires `ROLE_DEL` permission |

Deletes a role after validating it is not assigned to any user and not inherited by other roles. Uses optimistic locking. After deletion, cleans up all inheritance relationships where this role is a child.

---

## User Management

### Get User

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `GET /api/users/{id}`           |
| **Auth**     | Requires `USER_VIEW` permission |

Retrieves a single user by ID. Returns user details including ID, username, status, and version. Throws 404 if not found.

---

### Get User List

| Item         | Detail                                             |
| ------------ | -------------------------------------------------- |
| **Endpoint** | `GET /api/users`                                   |
| **Auth**     | Requires `USER_VIEW` permission                    |
| **Params**   | `username` (partial match), `status` (exact match) |

Queries users by optional conditions (username partial match, status exact match). Returns a list ordered by update time descending.

---

### Get User Page

| Item         | Detail                                                             |
| ------------ | ------------------------------------------------------------------ |
| **Endpoint** | `GET /api/users/page`                                              |
| **Auth**     | Requires `USER_VIEW` permission                                    |
| **Params**   | `page`, `size`, `username` (partial match), `status` (exact match) |

Queries users with pagination support. Accepts page number, page size, and optional filter conditions. Returns paged results with total count.

---

### Create User

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `POST /api/users`              |
| **Auth**     | Requires `USER_NEW` permission |

Creates a new user with username uniqueness validation. Password is encrypted via PasswordEncoder before storage. Supports simultaneous role assignment via roleIds. Returns the created user ID.

---

### Update User

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `PUT /api/users`                |
| **Auth**     | Requires `USER_EDIT` permission |

Updates user information. Username cannot be modified. Password is encrypted if provided, otherwise kept unchanged. Uses optimistic locking via version field. Evicts UserDetails cache after update.

---

### Delete User

| Item         | Detail                         |
| ------------ | ------------------------------ |
| **Endpoint** | `DELETE /api/users`            |
| **Auth**     | Requires `USER_DEL` permission |

Deletes a user with optimistic locking. Evicts UserDetails cache after deletion to ensure consistency.

---

## Common

### Get Enum List

| Item         | Detail                                                |
| ------------ | ----------------------------------------------------- |
| **Endpoint** | `GET /api/common/enums`                               |
| **Auth**     | Public (No authentication required)                   |
| **Params**   | `type` — supported values: `user-status`, `role-code` |

Returns enumeration data list by type. Supports `user-status` (user status options) and `role-code` (role code options). Each item contains `code` and `name` fields.

---

### Get Select Options

| Item         | Detail                                                                                            |
| ------------ | ------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/common/select-options`                                                                  |
| **Auth**     | Public (No authentication required)                                                               |
| **Params**   | `type` — dynamically registered via `SelectOptionProvider` implementations (e.g., `role`, `user`) |

Returns select option list for frontend dropdown/select components. Provider implementations are auto-discovered via Spring DI. To add a new type, simply create a new `SelectOptionProvider` bean — no controller modification required. Each option contains `value` and `label`.

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
| **权限**     | 需要 `ROLE_VIEW` 权限 |

根据 ID 从数据库中获取单个角色。返回角色详情，包括 ID、名称、代码、描述和版本号。未找到时抛出 404 异常。

---

### 获取角色列表

| 项目         | 详情                                                                      |
| ------------ | ------------------------------------------------------------------------- |
| **接口地址** | `GET /api/roles`                                                          |
| **权限**     | 需要 `ROLE_VIEW` 权限                                                     |
| **参数**     | `roleName`（模糊匹配）、`roleCode`（前缀匹配）、`description`（模糊匹配） |

根据可选条件查询角色（名称模糊匹配、代码前缀匹配、描述模糊匹配）。返回角色列表，包含各角色的直接父角色 ID，通过单次批量查询解析以避免 N+1 问题。结果按更新时间降序排列。

---

### 创建角色

| 项目         | 详情                 |
| ------------ | -------------------- |
| **接口地址** | `POST /api/roles`    |
| **权限**     | 需要 `ROLE_NEW` 权限 |

使用提供的名称、描述和可选的父角色 ID 创建新角色。角色代码不是必需的 - 它仅用于基础角色。自定义角色通过继承父角色获得权限。返回创建的角色 ID。

**请求体：**

- `roleName`（必填）：角色名称
- `description`（可选）：角色描述
- `parentRoleIds`（可选）：父角色 ID 列表，用于继承关系

---

### 更新角色

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `PUT /api/roles`      |
| **权限**     | 需要 `ROLE_EDIT` 权限 |

更新现有角色的名称、描述和继承关系。通过版本字段使用乐观锁确保并发安全。如果角色被其他事务修改则抛出异常。

**请求体：**

- `id`（必填）：角色 ID
- `roleName`（必填）：角色名称
- `description`（可选）：角色描述
- `parentRoleIds`（可选）：父角色 ID 列表，用于继承关系（替换现有关系）
- `version`（必填）：版本号，用于乐观锁

---

### 删除角色

| 项目         | 详情                 |
| ------------ | -------------------- |
| **接口地址** | `DELETE /api/roles`  |
| **权限**     | 需要 `ROLE_DEL` 权限 |

验证角色未被任何用户使用且未被其他角色继承后删除角色。使用乐观锁。删除后清理该角色作为子角色的所有继承关系。

---

## 用户管理

### 获取用户详情

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `GET /api/users/{id}` |
| **权限**     | 需要 `USER_VIEW` 权限 |

根据 ID 获取单个用户。返回用户详情，包括 ID、用户名、状态和版本号。未找到时抛出 404 异常。

---

### 获取用户列表

| 项目         | 详情                                         |
| ------------ | -------------------------------------------- |
| **接口地址** | `GET /api/users`                             |
| **权限**     | 需要 `USER_VIEW` 权限                        |
| **参数**     | `username`（模糊匹配）、`status`（精确匹配） |

根据可选条件查询用户（用户名模糊匹配、状态精确匹配）。返回按更新时间降序排列的列表。

---

### 获取用户分页

| 项目         | 详情                                                         |
| ------------ | ------------------------------------------------------------ |
| **接口地址** | `GET /api/users/page`                                        |
| **权限**     | 需要 `USER_VIEW` 权限                                        |
| **参数**     | `page`、`size`、`username`（模糊匹配）、`status`（精确匹配） |

支持分页查询用户。接受页码、页大小和可选过滤条件。返回带有总数的分页结果。

---

### 创建用户

| 项目         | 详情                 |
| ------------ | -------------------- |
| **接口地址** | `POST /api/users`    |
| **权限**     | 需要 `USER_NEW` 权限 |

创建新用户，验证用户名唯一性。密码通过 PasswordEncoder 加密后存储。支持通过 roleIds 同时分配角色。返回创建的用户 ID。

---

### 更新用户

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `PUT /api/users`      |
| **权限**     | 需要 `USER_EDIT` 权限 |

更新用户信息。用户名不可修改。如果提供密码则加密存储，否则保持不变。通过版本字段使用乐观锁。更新后清除 UserDetails 缓存。

---

### 删除用户

| 项目         | 详情                 |
| ------------ | -------------------- |
| **接口地址** | `DELETE /api/users`  |
| **权限**     | 需要 `USER_DEL` 权限 |

使用乐观锁删除用户。删除后清除 UserDetails 缓存以确保一致性。

---

## 公共接口

### 获取枚举列表

| 项目         | 详情                                        |
| ------------ | ------------------------------------------- |
| **接口地址** | `GET /api/common/enums`                     |
| **权限**     | 公开（无需认证）                            |
| **参数**     | `type` — 支持值：`user-status`、`role-code` |

根据类型返回枚举数据列表。支持 `user-status`（用户状态选项）和 `role-code`（角色代码选项）。每项包含 `code` 和 `name` 字段。

---

### 获取下拉选项

| 项目         | 详情                                                                   |
| ------------ | ---------------------------------------------------------------------- |
| **接口地址** | `GET /api/common/select-options`                                       |
| **权限**     | 公开（无需认证）                                                       |
| **参数**     | `type` — 通过 `SelectOptionProvider` 实现动态注册（如 `role`、`user`） |

返回前端下拉/选择组件的选项列表。Provider 实现通过 Spring DI 自动发现。添加新类型只需创建新的 `SelectOptionProvider` Bean，无需修改控制器。每个选项包含 `value` 和 `label`。

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
| **認可**           | `ROLE_VIEW` 権限が必要 |

ID でデータベースから単一のロールを取得します。ID、名前、コード、説明、バージョンを含むロール詳細を返します。見つからない場合は 404 例外をスローします。

---

### ロール一覧取得

| 項目               | 詳細                                                                      |
| ------------------ | ------------------------------------------------------------------------- |
| **エンドポイント** | `GET /api/roles`                                                          |
| **認可**           | `ROLE_VIEW` 権限が必要                                                    |
| **パラメータ**     | `roleName`（部分一致）、`roleCode`（前方一致）、`description`（部分一致） |

オプション条件（名前部分一致、コード前方一致、説明部分一致）でロールを検索します。各ロールの直接親ロール ID を含むリストを返し、N+1 問題を回避するため単一バッチクエリで解決します。結果は更新日時の降順で並べられます。

---

### ロール作成

| 項目               | 詳細                  |
| ------------------ | --------------------- |
| **エンドポイント** | `POST /api/roles`     |
| **認可**           | `ROLE_NEW` 権限が必要 |

提供された名前、説明、およびオプションの親ロール ID で新しいロールを作成します。ロールコードは必須ではありません - ベースロールのみで使用されます。カスタムロールは親ロールから権限を継承します。作成されたロール ID を返します。

**リクエストボディ：**

- `roleName`（必須）：ロール名
- `description`（オプション）：ロールの説明
- `parentRoleIds`（オプション）：継承用の親ロール ID リスト

---

### ロール更新

| 項目               | 詳細                   |
| ------------------ | ---------------------- |
| **エンドポイント** | `PUT /api/roles`       |
| **認可**           | `ROLE_EDIT` 権限が必要 |

既存ロールの名前、説明、および継承関係を更新します。バージョンフィールドによる楽観的ロックで並行処理の安全性を確保します。他のトランザクションによって変更された場合は例外をスローします。

**リクエストボディ：**

- `id`（必須）：ロール ID
- `roleName`（必須）：ロール名
- `description`（オプション）：ロールの説明
- `parentRoleIds`（オプション）：継承用の親ロール ID リスト（既存の関係を置換）
- `version`（必須）：楽観的ロック用のバージョン

---

### ロール削除

| 項目               | 詳細                  |
| ------------------ | --------------------- |
| **エンドポイント** | `DELETE /api/roles`   |
| **認可**           | `ROLE_DEL` 権限が必要 |

ロールがユーザーに割り当てられておらず、他のロールに継承されていないことを検証後に削除します。楽観的ロックを使用します。削除後、このロールが子ロールである全ての継承関係をクリーンアップします。

---

## ユーザー管理

### ユーザー詳細取得

| 項目               | 詳細                   |
| ------------------ | ---------------------- |
| **エンドポイント** | `GET /api/users/{id}`  |
| **認可**           | `USER_VIEW` 権限が必要 |

ID でユーザーを取得します。ID、ユーザー名、ステータス、バージョンを含むユーザー詳細を返します。見つからない場合は 404 例外をスローします。

---

### ユーザー一覧取得

| 項目               | 詳細                                         |
| ------------------ | -------------------------------------------- |
| **エンドポイント** | `GET /api/users`                             |
| **認可**           | `USER_VIEW` 権限が必要                       |
| **パラメータ**     | `username`（部分一致）、`status`（完全一致） |

オプション条件（ユーザー名部分一致、ステータス完全一致）でユーザーを検索します。更新日時の降順でリストを返します。

---

### ユーザーページング取得

| 項目               | 詳細                                                         |
| ------------------ | ------------------------------------------------------------ |
| **エンドポイント** | `GET /api/users/page`                                        |
| **認可**           | `USER_VIEW` 権限が必要                                       |
| **パラメータ**     | `page`、`size`、`username`（部分一致）、`status`（完全一致） |

ページネーション付きでユーザーを検索します。ページ番号、ページサイズ、オプションのフィルター条件を受け付けます。総数を含むページング結果を返します。

---

### ユーザー作成

| 項目               | 詳細                  |
| ------------------ | --------------------- |
| **エンドポイント** | `POST /api/users`     |
| **認可**           | `USER_NEW` 権限が必要 |

ユーザー名の一意性を検証して新規ユーザーを作成します。パスワードは PasswordEncoder で暗号化して保存します。roleIds による同時ロール割り当てをサポートします。作成されたユーザー ID を返します。

---

### ユーザー更新

| 項目               | 詳細                   |
| ------------------ | ---------------------- |
| **エンドポイント** | `PUT /api/users`       |
| **認可**           | `USER_EDIT` 権限が必要 |

ユーザー情報を更新します。ユーザー名は変更できません。パスワードが提供された場合は暗号化して保存し、そうでなければ変更しません。バージョンフィールドによる楽観的ロックを使用します。更新後に UserDetails キャッシュを無効化します。

---

### ユーザー削除

| 項目               | 詳細                  |
| ------------------ | --------------------- |
| **エンドポイント** | `DELETE /api/users`   |
| **認可**           | `USER_DEL` 権限が必要 |

楽観的ロックを使用してユーザーを削除します。削除後に UserDetails キャッシュを無効化して整合性を確保します。

---

## 共通

### 列挙リスト取得

| 項目               | 詳細                                            |
| ------------------ | ----------------------------------------------- |
| **エンドポイント** | `GET /api/common/enums`                         |
| **認可**           | 公開（認証不要）                                |
| **パラメータ**     | `type` — サポート値：`user-status`、`role-code` |

タイプ別に列挙データリストを返します。`user-status`（ユーザーステータスオプション）と `role-code`（ロールコードオプション）をサポートします。各項目には `code` と `name` フィールドが含まれます。

---

### セレクトオプション取得

| 項目               | 詳細                                                                     |
| ------------------ | ------------------------------------------------------------------------ |
| **エンドポイント** | `GET /api/common/select-options`                                         |
| **認可**           | 公開（認証不要）                                                         |
| **パラメータ**     | `type` — `SelectOptionProvider` 実装により動的登録（例：`role`、`user`） |

フロントエンドのドロップダウン/セレクトコンポーネント用のオプションリストを返します。Provider 実装は Spring DI により自動検出されます。新しいタイプを追加するには、新しい `SelectOptionProvider` Bean を作成するだけで、コントローラーの変更は不要です。各オプションには `value` と `label` が含まれます。
