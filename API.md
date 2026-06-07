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

## Organization — Department

### Get Department

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `GET /api/departments/{id}`     |
| **Auth**     | Requires `DEPT_READ` permission |

Retrieves a single department by ID. Returns `id`, `deptName`, `deptCode`, `parentId` (0 for top level), `leaderEmployeeId`, `sortOrder`, `status`, and `version`. Throws 404 if not found.

---

### Get Department Tree

| Item         | Detail                          |
| ------------ | ------------------------------- |
| **Endpoint** | `GET /api/departments/tree`     |
| **Auth**     | Requires `DEPT_READ` permission |

Returns the full department hierarchy as a nested tree. All departments are loaded in a single query and assembled in memory into nested `children` arrays. Siblings are ordered by `sortOrder` ascending (nulls last), then by `id` ascending. Orphaned nodes (whose parent is missing or soft-deleted) are surfaced as top-level nodes so they remain visible. Each node carries the same fields as the detail endpoint plus a `children` array.

---

### Create Department

| Item         | Detail                           |
| ------------ | -------------------------------- |
| **Endpoint** | `POST /api/departments`          |
| **Auth**     | Requires `DEPT_WRITE` permission |

Creates a new department. Validates parent existence (unless top-level) and department code uniqueness before inserting. A null `parentId` is normalized to `0` (top level). Returns the created department ID.

**Request Body:**

- `deptName` (required): Department name, max 64 characters
- `deptCode` (optional): Department code, max 64 characters (must be unique)
- `parentId` (optional): Parent department ID, `0` or omitted for top level
- `leaderEmployeeId` (optional): Department head employee ID
- `sortOrder` (optional): Sort order among siblings
- `status` (optional): Status (`0` disabled / `1` enabled)

---

### Update Department

| Item         | Detail                           |
| ------------ | -------------------------------- |
| **Endpoint** | `PUT /api/departments`           |
| **Auth**     | Requires `DEPT_WRITE` permission |

Updates an existing department. Validates parent existence, code uniqueness (excluding itself), and that the new parent does not create a cycle (a department cannot become a child of itself or any of its descendants). Uses optimistic locking via the `version` field.

**Request Body:**

- `id` (required): Department ID
- `deptName` (required): Department name, max 64 characters
- `deptCode` (optional): Department code, max 64 characters
- `parentId` (optional): Parent department ID (`0` for top level)
- `leaderEmployeeId` (optional): Department head employee ID
- `sortOrder` (optional): Sort order among siblings
- `status` (optional): Status (`0` disabled / `1` enabled)
- `version` (required): Version for optimistic locking

---

### Delete Department

| Item         | Detail                            |
| ------------ | --------------------------------- |
| **Endpoint** | `DELETE /api/departments`         |
| **Auth**     | Requires `DEPT_DELETE` permission |

Deletes a department after validating it has no sub-departments and no assigned employees. Uses optimistic locking via an explicit version condition.

**Request Body:**

- `id` (required): Department ID
- `version` (required): Version for optimistic locking

---

## Organization — Position

### Get Position

| Item         | Detail                              |
| ------------ | ----------------------------------- |
| **Endpoint** | `GET /api/positions/{id}`           |
| **Auth**     | Requires `POSITION_READ` permission |

Retrieves a single position by ID. Returns `id`, `positionName`, `positionCode`, `sortOrder`, `status`, and `version`. Throws 404 if not found.

---

### Get Position List

| Item         | Detail                                                 |
| ------------ | ------------------------------------------------------ |
| **Endpoint** | `GET /api/positions`                                   |
| **Auth**     | Requires `POSITION_READ` permission                    |
| **Params**   | `positionName` (partial match), `status` (exact match) |

Queries positions by optional conditions. Results are ordered by `sortOrder` ascending, then by `id` ascending. Each item includes `id`, `positionName`, `positionCode`, `sortOrder`, `status`, and `version`.

---

### Create Position

| Item         | Detail                               |
| ------------ | ------------------------------------ |
| **Endpoint** | `POST /api/positions`                |
| **Auth**     | Requires `POSITION_WRITE` permission |

Creates a new position. Validates position code uniqueness before inserting. Returns the created position ID.

**Request Body:**

- `positionName` (required): Position name, max 64 characters
- `positionCode` (optional): Position code, max 64 characters (must be unique)
- `sortOrder` (optional): Sort order
- `status` (optional): Status (`0` disabled / `1` enabled)

---

### Update Position

| Item         | Detail                               |
| ------------ | ------------------------------------ |
| **Endpoint** | `PUT /api/positions`                 |
| **Auth**     | Requires `POSITION_WRITE` permission |

Updates an existing position. Validates code uniqueness (excluding itself). Uses optimistic locking via the `version` field.

**Request Body:**

- `id` (required): Position ID
- `positionName` (required): Position name, max 64 characters
- `positionCode` (optional): Position code, max 64 characters
- `sortOrder` (optional): Sort order
- `status` (optional): Status (`0` disabled / `1` enabled)
- `version` (required): Version for optimistic locking

---

### Delete Position

| Item         | Detail                                |
| ------------ | ------------------------------------- |
| **Endpoint** | `DELETE /api/positions`               |
| **Auth**     | Requires `POSITION_DELETE` permission |

Deletes a position after validating it is not assigned to any employee. Uses optimistic locking via an explicit version condition.

**Request Body:**

- `id` (required): Position ID
- `version` (required): Version for optimistic locking

---

## Organization — Employee

### Get Employee

| Item         | Detail                              |
| ------------ | ----------------------------------- |
| **Endpoint** | `GET /api/employees/{id}`           |
| **Auth**     | Requires `EMPLOYEE_READ` permission |

Retrieves a single employee by ID. Returns `id`, `userId` (linked login account), `employeeNo`, `realName`, `deptId`, `positionId`, `managerId`, `mobile`, `email`, `gender`, `idCardNo`, `birthday`, `hireDate`, `status`, and `version`. Throws 404 if not found.

---

### Get Employee Page

| Item         | Detail                                                                                              |
| ------------ | --------------------------------------------------------------------------------------------------- |
| **Endpoint** | `GET /api/employees/page`                                                                           |
| **Auth**     | Requires `EMPLOYEE_READ` permission                                                                 |
| **Params**   | `page`, `size`, `realName` (partial match), `employeeNo` (partial match), `deptId`, `status`        |

Queries employees with pagination support. Supports dynamic sorting via `sortBy`/`sortOrder` over a whitelist (`id`, `employee_no`, `real_name`, `dept_id`, `hire_date`, `create_at`, `update_at`), defaulting to `update_at` descending. Each item includes `id`, `userId`, `employeeNo`, `realName`, `deptId`, `positionId`, `mobile`, `email`, `hireDate`, `status`, and `version`.

---

### Create Employee

| Item         | Detail                               |
| ------------ | ------------------------------------ |
| **Endpoint** | `POST /api/employees`                |
| **Auth**     | Requires `EMPLOYEE_WRITE` permission |

Creates a new employee **together with a linked login account** in a single transaction. Validates employee number uniqueness, provisions the login account (generates a random password, sets the account to inactive, sends the welcome email after commit, assigns the given roles), then links the resulting user ID to the employee. If either side fails, both are rolled back atomically — no orphan employee or account is left behind.

**Request Body:**

- `username` (required): Login username, alphanumeric and underscore only (`^[a-zA-Z0-9_]+$`), max 64 characters
- `employeeNo` (optional): Employee number, max 32 characters (must be unique)
- `realName` (required): Real name, max 64 characters
- `deptId` (optional): Department ID
- `positionId` (optional): Position ID
- `managerId` (optional): Direct supervisor employee ID
- `mobile` (optional): Mobile phone, max 20 characters
- `email` (required): Email address, valid email, max 128 characters (used for the login account)
- `gender` (optional): Gender (`0` unknown / `1` male / `2` female)
- `idCardNo` (optional): ID card number, max 32 characters
- `birthday` (optional): Date of birth (`yyyy-MM-dd`)
- `hireDate` (optional): Hire date (`yyyy-MM-dd`)
- `status` (optional): Employment status (`0` left / `1` active / `2` on leave)
- `roleIds` (optional): Role IDs to assign to the auto-created account

Returns the created employee ID.

---

### Update Employee

| Item         | Detail                               |
| ------------ | ------------------------------------ |
| **Endpoint** | `PUT /api/employees`                 |
| **Auth**     | Requires `EMPLOYEE_WRITE` permission |

Updates an employee profile only. The linked login account (username, password, roles) is managed separately via the User Management module. Validates employee number uniqueness (excluding itself). Uses optimistic locking via the `version` field.

**Request Body:**

- `id` (required): Employee ID
- `employeeNo` (optional): Employee number, max 32 characters
- `realName` (required): Real name, max 64 characters
- `deptId` (optional): Department ID
- `positionId` (optional): Position ID
- `managerId` (optional): Direct supervisor employee ID
- `mobile` (optional): Mobile phone, max 20 characters
- `email` (optional): Email address, valid email, max 128 characters
- `gender` (optional): Gender (`0` unknown / `1` male / `2` female)
- `idCardNo` (optional): ID card number, max 32 characters
- `birthday` (optional): Date of birth (`yyyy-MM-dd`)
- `hireDate` (optional): Hire date (`yyyy-MM-dd`)
- `status` (optional): Employment status (`0` left / `1` active / `2` on leave)
- `version` (required): Version for optimistic locking

---

### Delete Employee

| Item         | Detail                                |
| ------------ | ------------------------------------- |
| **Endpoint** | `DELETE /api/employees`               |
| **Auth**     | Requires `EMPLOYEE_DELETE` permission |

Deletes an employee profile using optimistic locking via an explicit version condition. The linked login account is intentionally left untouched and must be managed via the User Management module.

**Request Body:**

- `id` (required): Employee ID
- `version` (required): Version for optimistic locking

---

## Workflow — Leave Approval

> The approval flow is driven by the Flowable BPMN engine. Business data lives in `oa_leave_request` (MyBatis-Plus); Flowable owns the process instance and the approval task. Approvers are resolved dynamically from the organization tables, and their login usernames are passed to Flowable as task assignees.
>
> **Approval levels (threshold-based):**
>
> - `days < 3` — single level: direct manager only (`mst_employee.manager_id`).
> - `days >= 3` — two levels: direct manager → department leader (`mst_department.leader_employee_id`). The business record stays `pending` after the first approval until the process actually ends.
>
> **Annual leave (`leaveType = 1`) quota rules:** Quota is granted on each employment anniversary, tiered by tenure at grant date (1–3 years → 5 days, 4–6 years → 7 days, 7+ years → 9 days). Each grant batch is valid for 24 months and consumed FIFO (earliest-granted first). On submit, annual leave quota is consumed within the same transaction; on reject/withdraw it is refunded to the exact original batches. Other leave types do not check or consume quota.

### Submit Leave Request

| Item         | Detail                            |
| ------------ | --------------------------------- |
| **Endpoint** | `POST /api/leaves`                |
| **Auth**     | Requires `LEAVE_APPLY` permission |

Submits a new leave request and starts the approval process in one transaction. The applicant is resolved from the authenticated user; the direct manager is resolved from the applicant's `managerId`. For `days >= 3`, the department leader is also resolved (the applicant's department must have a leader). For annual leave, the available quota is checked and consumed FIFO. Returns the created leave request ID.

**Validation & failures:**

- `leaveType` must be a defined type, otherwise `error.leave.invalid_type`.
- `days` must be a positive multiple of `0.5`, otherwise `error.leave.days_not_half_unit`.
- No direct manager → `error.leave.no_manager`.
- `days >= 3` but the department has no leader → `error.leave.no_dept_leader`.
- Annual leave exceeding the available balance → `error.leave.insufficient_annual`.

The entire operation (quota consumption + record insert + process start) is transactional: any failure rolls back all of it.

**Request Body:**

- `leaveType` (required): Leave type (`1` annual / `2` sick / `3` personal / `4` marriage / `5` maternity / `9` other), max 2 characters
- `startTime` (required): Leave start time (`yyyy-MM-dd'T'HH:mm:ss`)
- `endTime` (required): Leave end time (`yyyy-MM-dd'T'HH:mm:ss`)
- `days` (required): Number of leave days, must be a positive multiple of `0.5`
- `reason` (optional): Leave reason, max 500 characters

---

### Get My Pending Tasks

| Item         | Detail                              |
| ------------ | ----------------------------------- |
| **Endpoint** | `GET /api/leaves/tasks`             |
| **Auth**     | Requires `LEAVE_APPROVE` permission |

Lists the pending approval tasks assigned to the current user. Queries Flowable for tasks whose assignee equals the current user's username, then loads the underlying leave request business data for each. Each item includes `leaveId`, `employeeId`, `leaveType`, `startTime`, `endTime`, `days`, `reason`, and `createAt`.

---

### Review Leave Request

| Item         | Detail                              |
| ------------ | ----------------------------------- |
| **Endpoint** | `POST /api/leaves/review`           |
| **Auth**     | Requires `LEAVE_APPROVE` permission |

Approves or rejects a leave request. The current user must be the assignee of the pending Flowable task. Completes the task with the `approved` variable (which drives the BPMN exclusive gateway), then synchronizes the business status and persists the approval comment.

- **Approve, single-level (`days < 3`)** → process ends → status `1` approved.
- **Approve, first level of two (`days >= 3`)** → process advances to the department leader → status stays `0` pending (comment recorded).
- **Approve, final level** → process ends → status `1` approved.
- **Reject (any level)** → process ends → status `2` rejected. For annual leave, the consumed quota is refunded.

Fails if the leave request is not in a pending state or has no pending task.

**Request Body:**

- `id` (required): Leave request ID
- `approved` (required): Approval decision (`true` = approve, `false` = reject)
- `comment` (optional): Approver comment, max 500 characters

---

### Withdraw Leave Request

| Item         | Detail                            |
| ------------ | --------------------------------- |
| **Endpoint** | `POST /api/leaves/withdraw`       |
| **Auth**     | Requires `LEAVE_APPLY` permission |

Withdraws a pending leave request. Only the applicant may withdraw their own request, and only while it is still pending. Deletes the Flowable process instance and marks the business record as withdrawn (`3`). For annual leave, the consumed quota is refunded to the exact original batches. Fails if the caller is not the owner or the request is not pending.

**Request Body:**

- `id` (required): Leave request ID

---

### Get My Annual Leave Balance

| Item         | Detail                            |
| ------------ | --------------------------------- |
| **Endpoint** | `GET /api/leaves/annual-balance`  |
| **Auth**     | Requires `LEAVE_APPLY` permission |

Returns the current user's annual-leave balance as of today. Materializes any due (non-expired) grant batches on first access (lazy grant), then returns the total available days plus the per-batch breakdown. Returns:

- `employeeId` — the current user's employee ID
- `totalAvailable` — total available annual-leave days
- `batches[]` — active grant batches, earliest grant date first, each with `grantDate`, `expireDate`, `grantedDays`, `usedDays`, `remainingDays`

---

### Get Annual Leave Balance by Employee

| Item         | Detail                                       |
| ------------ | -------------------------------------------- |
| **Endpoint** | `GET /api/leaves/annual-balance/{employeeId}` |
| **Auth**     | Requires `LEAVE_READ` permission             |

Privileged view of another employee's annual-leave balance. Same response shape as the self endpoint. Throws 404 if the employee is not found.

---

### Get Leave Request

| Item         | Detail                           |
| ------------ | -------------------------------- |
| **Endpoint** | `GET /api/leaves/{id}`           |
| **Auth**     | Requires `LEAVE_READ` permission |

Retrieves a single leave request by ID. Returns `id`, `employeeId`, `leaveType`, `startTime`, `endTime`, `days`, `reason`, `status` (`0` pending / `1` approved / `2` rejected / `3` withdrawn), `processInstanceId`, `approverId`, `approvalComment`, and `version`. Throws 404 if not found.

---

### Get Leave Request Page

| Item         | Detail                                                          |
| ------------ | --------------------------------------------------------------- |
| **Endpoint** | `GET /api/leaves/page`                                          |
| **Auth**     | Requires `LEAVE_READ` permission                                |
| **Params**   | `page`, `size`, `employeeId`, `leaveType`, `status`             |

Queries leave requests with pagination support. Supports dynamic sorting via `sortBy`/`sortOrder` over a whitelist (`id`, `employee_id`, `leave_type`, `status`, `create_at`, `update_at`), defaulting to `create_at` descending. Each item includes `id`, `employeeId`, `leaveType`, `startTime`, `endTime`, `days`, `status`, `approverId`, `createAt`, and `version`.

---

## Common

### Get Enum List

| Item         | Detail                                                |
| ------------ | ----------------------------------------------------- |
| **Endpoint** | `GET /api/common/enums`                               |
| **Auth**     | Public (No authentication required)                   |
| **Params**   | `type` — supported values: `user-status`, `role-code`, `module`, `sub-module`, `leave-type` |

Returns enumeration data list by type. Supports `user-status` (user status options), `role-code` (role code options), `module` (module options), `sub-module` (sub-module options), and `leave-type` (leave type options). Each item contains `code` and `name` fields.

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

### Upload File

| Item         | Detail                                                                                                      |
| ------------ | ----------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `POST /api/common/files`                                                                                    |
| **Auth**     | Requires `FILE_WRITE` permission                                                                            |
| **Params**   | `file` (required) — multipart file, `associateType` (optional) — association type, `associateId` (optional) — associated entity ID |
| **Content-Type** | `multipart/form-data`                                                                                   |

Uploads a single file to the server. The file is stored under a date-based subdirectory (`yyyy/MM/dd`) with a UUID-based filename to prevent conflicts and path traversal attacks. MD5 hash is computed during upload via `DigestInputStream` and stored in the database for integrity verification. File type is auto-detected from extension (1=image, 2=document, 3=archive, 9=other). Extension must be in the allowed list configured in `app.upload.allowed-extensions`. File size must not exceed `app.upload.max-file-size-mb`.

Returns: `id`, `storedName`, `originalFilename`, `datePath`, `dateUrl`, `size`, `contentType`, `md5`.

---

### Upload Files (Batch)

| Item         | Detail                                                                                                          |
| ------------ | --------------------------------------------------------------------------------------------------------------- |
| **Endpoint** | `POST /api/common/files/batch`                                                                                  |
| **Auth**     | Requires `FILE_WRITE` permission                                                                                |
| **Params**   | `files` (required) — multipart file array, `associateType` (optional), `associateId` (optional)                |
| **Content-Type** | `multipart/form-data`                                                                                       |

Uploads multiple files in a single request. Each file is processed independently with the same rules as single upload. Total number of files must not exceed `app.upload.max-files-per-request`.

Returns: Array of `id`, `storedName`, `originalFilename`, `datePath`, `dateUrl`, `size`, `contentType`, `md5`.

---

### Download File

| Item          | Detail                                           |
| ------------- | ------------------------------------------------- |
| **Endpoint**  | `GET /api/common/files/{dateUrl}/{storedName}`   |
| **Auth**      | Requires `FILE_READ` permission                  |
| **Response**  | Binary file content with `Content-Disposition: attachment` |

Downloads a file by its date URL and stored name. The server verifies MD5 integrity before serving: if the file on disk does not match the stored MD5 hash, a `FILE_MD5_MISMATCH` error is returned. The response includes the `X-File-MD5` header containing the stored MD5 hash, allowing client-side integrity verification.

---

### Verify File Integrity

| Item         | Detail                                                    |
| ------------ | --------------------------------------------------------- |
| **Endpoint** | `GET /api/common/files/{dateUrl}/{storedName}/verify`     |
| **Auth**     | Requires `FILE_READ` permission                           |

Verifies that the file on disk matches the stored MD5 hash in the database. Recomputes the MD5 of the physical file and compares it with the database record.

Returns: `{ "match": true/false, "storedMd5": "..." }`

---

### Delete File

| Item         | Detail                                           |
| ------------ | ------------------------------------------------- |
| **Endpoint** | `DELETE /api/common/files/{dateUrl}/{storedName}` |
| **Auth**     | Requires `FILE_DELETE` permission                |

Deletes a file from disk and logically deletes its database record. Physical file is removed from the filesystem, and the database record is soft-deleted (`deleted` flag set to 1) via `@TableLogic`.

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

## 组织架构 — 部门

### 获取部门详情

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `GET /api/departments/{id}` |
| **权限**     | 需要 `DEPT_READ` 权限 |

根据 ID 获取单个部门。返回 `id`、`deptName`、`deptCode`、`parentId`（顶级为 0）、`leaderEmployeeId`、`sortOrder`、`status`、`version`。未找到时抛出 404 异常。

---

### 获取部门树

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `GET /api/departments/tree` |
| **权限**     | 需要 `DEPT_READ` 权限 |

返回完整的部门层级树形结构。一次性查询所有部门，在内存中组装为嵌套的 `children` 数组。同级按 `sortOrder` 升序排列（null 排最后），再按 `id` 升序。孤儿节点（父级缺失或被逻辑删除）会上浮为顶级节点以保持可见。每个节点包含与详情接口相同的字段，外加 `children` 数组。

---

### 创建部门

| 项目         | 详情                   |
| ------------ | ---------------------- |
| **接口地址** | `POST /api/departments` |
| **权限**     | 需要 `DEPT_WRITE` 权限 |

创建新部门。插入前校验父部门存在性（非顶级时）和部门编码唯一性。`parentId` 为 null 时规范化为 `0`（顶级）。返回创建的部门 ID。

**请求体：**

- `deptName`（必填）：部门名称，最长 64 字符
- `deptCode`（可选）：部门编码，最长 64 字符（须唯一）
- `parentId`（可选）：父部门 ID，`0` 或不传表示顶级
- `leaderEmployeeId`（可选）：部门负责人员工 ID
- `sortOrder`（可选）：同级排序
- `status`（可选）：状态（`0` 停用 / `1` 启用）

---

### 更新部门

| 项目         | 详情                   |
| ------------ | ---------------------- |
| **接口地址** | `PUT /api/departments` |
| **权限**     | 需要 `DEPT_WRITE` 权限 |

更新现有部门。校验父部门存在性、编码唯一性（排除自身），以及新父级不会形成循环（部门不能成为自己或其后代的子级）。通过 `version` 字段使用乐观锁。

**请求体：**

- `id`（必填）：部门 ID
- `deptName`（必填）：部门名称，最长 64 字符
- `deptCode`（可选）：部门编码，最长 64 字符
- `parentId`（可选）：父部门 ID（`0` 表示顶级）
- `leaderEmployeeId`（可选）：部门负责人员工 ID
- `sortOrder`（可选）：同级排序
- `status`（可选）：状态（`0` 停用 / `1` 启用）
- `version`（必填）：版本号，用于乐观锁

---

### 删除部门

| 项目         | 详情                    |
| ------------ | ----------------------- |
| **接口地址** | `DELETE /api/departments` |
| **权限**     | 需要 `DEPT_DELETE` 权限 |

校验部门无子部门且无关联员工后删除。通过显式版本条件使用乐观锁。

**请求体：**

- `id`（必填）：部门 ID
- `version`（必填）：版本号，用于乐观锁

---

## 组织架构 — 岗位

### 获取岗位详情

| 项目         | 详情                      |
| ------------ | ------------------------- |
| **接口地址** | `GET /api/positions/{id}` |
| **权限**     | 需要 `POSITION_READ` 权限 |

根据 ID 获取单个岗位。返回 `id`、`positionName`、`positionCode`、`sortOrder`、`status`、`version`。未找到时抛出 404 异常。

---

### 获取岗位列表

| 项目         | 详情                                              |
| ------------ | ------------------------------------------------- |
| **接口地址** | `GET /api/positions`                              |
| **权限**     | 需要 `POSITION_READ` 权限                         |
| **参数**     | `positionName`（模糊匹配）、`status`（精确匹配）  |

根据可选条件查询岗位。结果按 `sortOrder` 升序、再按 `id` 升序排列。每项包含 `id`、`positionName`、`positionCode`、`sortOrder`、`status`、`version`。

---

### 创建岗位

| 项目         | 详情                       |
| ------------ | -------------------------- |
| **接口地址** | `POST /api/positions`      |
| **权限**     | 需要 `POSITION_WRITE` 权限 |

创建新岗位。插入前校验岗位编码唯一性。返回创建的岗位 ID。

**请求体：**

- `positionName`（必填）：岗位名称，最长 64 字符
- `positionCode`（可选）：岗位编码，最长 64 字符（须唯一）
- `sortOrder`（可选）：排序
- `status`（可选）：状态（`0` 停用 / `1` 启用）

---

### 更新岗位

| 项目         | 详情                       |
| ------------ | -------------------------- |
| **接口地址** | `PUT /api/positions`       |
| **权限**     | 需要 `POSITION_WRITE` 权限 |

更新现有岗位。校验编码唯一性（排除自身）。通过 `version` 字段使用乐观锁。

**请求体：**

- `id`（必填）：岗位 ID
- `positionName`（必填）：岗位名称，最长 64 字符
- `positionCode`（可选）：岗位编码，最长 64 字符
- `sortOrder`（可选）：排序
- `status`（可选）：状态（`0` 停用 / `1` 启用）
- `version`（必填）：版本号，用于乐观锁

---

### 删除岗位

| 项目         | 详情                        |
| ------------ | --------------------------- |
| **接口地址** | `DELETE /api/positions`     |
| **权限**     | 需要 `POSITION_DELETE` 权限 |

校验岗位未分配给任何员工后删除。通过显式版本条件使用乐观锁。

**请求体：**

- `id`（必填）：岗位 ID
- `version`（必填）：版本号，用于乐观锁

---

## 组织架构 — 员工

### 获取员工详情

| 项目         | 详情                      |
| ------------ | ------------------------- |
| **接口地址** | `GET /api/employees/{id}` |
| **权限**     | 需要 `EMPLOYEE_READ` 权限 |

根据 ID 获取单个员工。返回 `id`、`userId`（关联的登录账号）、`employeeNo`、`realName`、`deptId`、`positionId`、`managerId`、`mobile`、`email`、`gender`、`idCardNo`、`birthday`、`hireDate`、`status`、`version`。未找到时抛出 404 异常。

---

### 获取员工分页

| 项目         | 详情                                                                                          |
| ------------ | --------------------------------------------------------------------------------------------- |
| **接口地址** | `GET /api/employees/page`                                                                     |
| **权限**     | 需要 `EMPLOYEE_READ` 权限                                                                     |
| **参数**     | `page`、`size`、`realName`（模糊匹配）、`employeeNo`（模糊匹配）、`deptId`、`status`           |

支持分页查询员工。支持通过 `sortBy`/`sortOrder` 在白名单字段（`id`、`employee_no`、`real_name`、`dept_id`、`hire_date`、`create_at`、`update_at`）上动态排序，默认按 `update_at` 降序。每项包含 `id`、`userId`、`employeeNo`、`realName`、`deptId`、`positionId`、`mobile`、`email`、`hireDate`、`status`、`version`。

---

### 创建员工

| 项目         | 详情                       |
| ------------ | -------------------------- |
| **接口地址** | `POST /api/employees`      |
| **权限**     | 需要 `EMPLOYEE_WRITE` 权限 |

在单个事务中创建员工**并同步创建关联的登录账号**。校验工号唯一性，创建登录账号（生成随机密码、账号置为未激活、事务提交后发送欢迎邮件、分配指定角色），然后将生成的用户 ID 关联到员工。任一环节失败则两者原子回滚 —— 不会残留孤儿员工或孤儿账号。

**请求体：**

- `username`（必填）：登录用户名，仅允许字母、数字和下划线（`^[a-zA-Z0-9_]+$`），最长 64 字符
- `employeeNo`（可选）：工号，最长 32 字符（须唯一）
- `realName`（必填）：真实姓名，最长 64 字符
- `deptId`（可选）：部门 ID
- `positionId`（可选）：岗位 ID
- `managerId`（可选）：直属上级员工 ID
- `mobile`（可选）：手机号，最长 20 字符
- `email`（必填）：邮箱，合法邮箱格式，最长 128 字符（用于登录账号）
- `gender`（可选）：性别（`0` 未知 / `1` 男 / `2` 女）
- `idCardNo`（可选）：身份证号，最长 32 字符
- `birthday`（可选）：出生日期（`yyyy-MM-dd`）
- `hireDate`（可选）：入职日期（`yyyy-MM-dd`）
- `status`（可选）：在职状态（`0` 离职 / `1` 在职 / `2` 休假）
- `roleIds`（可选）：分配给自动创建账号的角色 ID 列表

返回创建的员工 ID。

---

### 更新员工

| 项目         | 详情                       |
| ------------ | -------------------------- |
| **接口地址** | `PUT /api/employees`       |
| **权限**     | 需要 `EMPLOYEE_WRITE` 权限 |

仅更新员工档案。关联的登录账号（用户名、密码、角色）由用户管理模块单独管理。校验工号唯一性（排除自身）。通过 `version` 字段使用乐观锁。

**请求体：**

- `id`（必填）：员工 ID
- `employeeNo`（可选）：工号，最长 32 字符
- `realName`（必填）：真实姓名，最长 64 字符
- `deptId`（可选）：部门 ID
- `positionId`（可选）：岗位 ID
- `managerId`（可选）：直属上级员工 ID
- `mobile`（可选）：手机号，最长 20 字符
- `email`（可选）：邮箱，合法邮箱格式，最长 128 字符
- `gender`（可选）：性别（`0` 未知 / `1` 男 / `2` 女）
- `idCardNo`（可选）：身份证号，最长 32 字符
- `birthday`（可选）：出生日期（`yyyy-MM-dd`）
- `hireDate`（可选）：入职日期（`yyyy-MM-dd`）
- `status`（可选）：在职状态（`0` 离职 / `1` 在职 / `2` 休假）
- `version`（必填）：版本号，用于乐观锁

---

### 删除员工

| 项目         | 详情                        |
| ------------ | --------------------------- |
| **接口地址** | `DELETE /api/employees`     |
| **权限**     | 需要 `EMPLOYEE_DELETE` 权限 |

通过显式版本条件使用乐观锁删除员工档案。关联的登录账号有意保留不动，须通过用户管理模块单独管理。

**请求体：**

- `id`（必填）：员工 ID
- `version`（必填）：版本号，用于乐观锁

---

## 工作流 — 请假审批

> 审批流程由 Flowable BPMN 引擎驱动。业务数据存于 `oa_leave_request`（MyBatis-Plus），Flowable 负责流程实例与审批任务。审批人由组织架构动态解析，其登录用户名作为审批任务负责人传给 Flowable。
>
> **审批层级（按天数阈值分级）：**
>
> - `days < 3` —— 一级：仅直属上级（`mst_employee.manager_id`）。
> - `days >= 3` —— 两级：直属上级 → 部门负责人（`mst_department.leader_employee_id`）。一级同意后业务状态仍为 `待审批`，直到流程真正结束。
>
> **年假（`leaveType = 1`）额度规则：** 额度按入职周年发放，按发放日工龄分级（满 1–3 年 → 5 天，4–6 年 → 7 天，7 年以上 → 9 天）。每批额度自发放日起 24 个月有效，按 FIFO（最早发放优先）扣减。提交时年假额度在同一事务内扣减；驳回/撤回时精确退还到原批次。其他请假类型不校验、不扣减额度。

### 提交请假申请

| 项目         | 详情                  |
| ------------ | --------------------- |
| **接口地址** | `POST /api/leaves`    |
| **权限**     | 需要 `LEAVE_APPLY` 权限 |

在单个事务中提交请假申请并启动审批流程。申请人由当前登录用户解析；直属上级由申请人的 `managerId` 解析。当 `days >= 3` 时还会解析部门负责人（申请人所在部门必须已配置负责人）。年假会校验并按 FIFO 扣减可用额度。返回创建的请假申请 ID。

**校验与失败：**

- `leaveType` 必须是已定义类型，否则 `error.leave.invalid_type`。
- `days` 必须是 `0.5` 的正整数倍，否则 `error.leave.days_not_half_unit`。
- 无直属上级 → `error.leave.no_manager`。
- `days >= 3` 但部门无负责人 → `error.leave.no_dept_leader`。
- 年假超过可用余额 → `error.leave.insufficient_annual`。

整个操作（额度扣减 + 插入记录 + 启动流程）在同一事务内，任一环节失败全部回滚。

**请求体：**

- `leaveType`（必填）：请假类型（`1` 年假 / `2` 病假 / `3` 事假 / `4` 婚假 / `5` 产假 / `9` 其他），最长 2 字符
- `startTime`（必填）：请假开始时间（`yyyy-MM-dd'T'HH:mm:ss`）
- `endTime`（必填）：请假结束时间（`yyyy-MM-dd'T'HH:mm:ss`）
- `days`（必填）：请假天数，必须是 `0.5` 的正整数倍
- `reason`（可选）：请假事由，最长 500 字符

---

### 获取我的待办任务

| 项目         | 详情                    |
| ------------ | ----------------------- |
| **接口地址** | `GET /api/leaves/tasks` |
| **权限**     | 需要 `LEAVE_APPROVE` 权限 |

列出分配给当前用户的待审批任务。从 Flowable 查询负责人为当前用户名的任务，再为每条任务加载底层请假申请的业务数据。每项包含 `leaveId`、`employeeId`、`leaveType`、`startTime`、`endTime`、`days`、`reason`、`createAt`。

---

### 审批请假申请

| 项目         | 详情                      |
| ------------ | ------------------------- |
| **接口地址** | `POST /api/leaves/review` |
| **权限**     | 需要 `LEAVE_APPROVE` 权限 |

同意或驳回请假申请。当前用户必须是待办 Flowable 任务的负责人。用 `approved` 变量完成任务（驱动 BPMN 排他网关），随后同步业务状态并持久化审批意见。

- **同意，一级（`days < 3`）** → 流程结束 → 状态 `1` 已批准。
- **同意，两级中的一级（`days >= 3`）** → 流程推进到部门负责人 → 状态仍为 `0` 待审批（记录意见）。
- **同意，最后一级** → 流程结束 → 状态 `1` 已批准。
- **驳回（任意层级）** → 流程结束 → 状态 `2` 已驳回。年假会退还已扣额度。

若请假申请不处于待审批状态或无待办任务则失败。

**请求体：**

- `id`（必填）：请假申请 ID
- `approved`（必填）：审批决定（`true` 同意，`false` 驳回）
- `comment`（可选）：审批意见，最长 500 字符

---

### 撤回请假申请

| 项目         | 详情                        |
| ------------ | --------------------------- |
| **接口地址** | `POST /api/leaves/withdraw` |
| **权限**     | 需要 `LEAVE_APPLY` 权限     |

撤回待审批的请假申请。仅申请人本人可撤回自己的申请，且仅在仍处于待审批状态时可撤回。删除 Flowable 流程实例，并将业务记录标记为已撤回（`3`）。年假会精确退还到原批次。若调用者非本人或申请不处于待审批状态则失败。

**请求体：**

- `id`（必填）：请假申请 ID

---

### 获取我的年假余额

| 项目         | 详情                             |
| ------------ | -------------------------------- |
| **接口地址** | `GET /api/leaves/annual-balance` |
| **权限**     | 需要 `LEAVE_APPLY` 权限          |

返回当前用户截至今日的年假余额。首次访问时物化尚未过期的应发批次（懒发放），然后返回总可用天数及各批次明细。返回：

- `employeeId` —— 当前用户的员工 ID
- `totalAvailable` —— 总可用年假天数
- `batches[]` —— 有效（未过期）发放批次，按发放日升序，每项含 `grantDate`、`expireDate`、`grantedDays`、`usedDays`、`remainingDays`

---

### 按员工获取年假余额

| 项目         | 详情                                          |
| ------------ | --------------------------------------------- |
| **接口地址** | `GET /api/leaves/annual-balance/{employeeId}` |
| **权限**     | 需要 `LEAVE_READ` 权限                        |

管理员查询他人年假余额。响应结构与"查自己"接口相同。员工不存在时抛出 404 异常。

---

### 获取请假申请详情

| 项目         | 详情                     |
| ------------ | ------------------------ |
| **接口地址** | `GET /api/leaves/{id}`   |
| **权限**     | 需要 `LEAVE_READ` 权限   |

根据 ID 获取单个请假申请。返回 `id`、`employeeId`、`leaveType`、`startTime`、`endTime`、`days`、`reason`、`status`（`0` 待审批 / `1` 已批准 / `2` 已驳回 / `3` 已撤回）、`processInstanceId`、`approverId`、`approvalComment`、`version`。未找到时抛出 404 异常。

---

### 获取请假申请分页

| 项目         | 详情                                                        |
| ------------ | ----------------------------------------------------------- |
| **接口地址** | `GET /api/leaves/page`                                      |
| **权限**     | 需要 `LEAVE_READ` 权限                                      |
| **参数**     | `page`、`size`、`employeeId`、`leaveType`、`status`         |

支持分页查询请假申请。支持通过 `sortBy`/`sortOrder` 在白名单字段（`id`、`employee_id`、`leave_type`、`status`、`create_at`、`update_at`）上动态排序，默认按 `create_at` 降序。每项包含 `id`、`employeeId`、`leaveType`、`startTime`、`endTime`、`days`、`status`、`approverId`、`createAt`、`version`。

---

## 公共接口

### 获取枚举列表

| 项目         | 详情                                        |
| ------------ | ------------------------------------------- |
| **接口地址** | `GET /api/common/enums`                     |
| **权限**     | 公开（无需认证）                            |
| **参数**     | `type` — 支持值：`user-status`、`role-code`、`module`、`sub-module`、`leave-type` |

根据类型返回枚举数据列表。支持 `user-status`（用户状态选项）、`role-code`（角色代码选项）、`module`（模块选项）、`sub-module`（子模块选项）和 `leave-type`（请假类型选项）。每项包含 `code` 和 `name` 字段。

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

### 上传文件

| 项目           | 详情                                                                                                    |
| -------------- | ------------------------------------------------------------------------------------------------------- |
| **接口地址**   | `POST /api/common/files`                                                                               |
| **权限**       | 需要 `FILE_WRITE` 权限                                                                                  |
| **参数**       | `file`（必填）— 上传文件，`associateType`（可选）— 关联类型，`associateId`（可选）— 关联业务实体 ID |
| **Content-Type** | `multipart/form-data`                                                                                |

上传单个文件到服务器。文件存储在按日期组织的子目录（`yyyy/MM/dd`）下，使用 UUID 文件名防止冲突和路径遍历攻击。上传过程中通过 `DigestInputStream` 计算 MD5 哈希并存储到数据库，用于完整性校验。文件类型根据扩展名自动检测（1=图片，2=文档，3=压缩包，9=其他）。扩展名必须在 `app.upload.allowed-extensions` 配置的允许列表中。文件大小不得超过 `app.upload.max-file-size-mb`。

返回：`id`、`storedName`、`originalFilename`、`datePath`、`dateUrl`、`size`、`contentType`、`md5`。

---

### 批量上传文件

| 项目           | 详情                                                                                              |
| -------------- | ------------------------------------------------------------------------------------------------- |
| **接口地址**   | `POST /api/common/files/batch`                                                                   |
| **权限**       | 需要 `FILE_WRITE` 权限                                                                            |
| **参数**       | `files`（必填）— 上传文件数组，`associateType`（可选），`associateId`（可选）                   |
| **Content-Type** | `multipart/form-data`                                                                          |

批量上传多个文件。每个文件独立处理，规则与单文件上传相同。文件总数不得超过 `app.upload.max-files-per-request`。

返回：`id`、`storedName`、`originalFilename`、`datePath`、`dateUrl`、`size`、`contentType`、`md5` 的数组。

---

### 下载文件

| 项目           | 详情                                                |
| -------------- | --------------------------------------------------- |
| **接口地址**   | `GET /api/common/files/{dateUrl}/{storedName}`      |
| **权限**       | 需要 `FILE_READ` 权限                               |
| **响应**       | 二进制文件内容，包含 `Content-Disposition: attachment` |

通过日期 URL 和存储文件名下载文件。服务器在返回文件前验证 MD5 完整性：如果磁盘文件与存储的 MD5 哈希不匹配，返回 `FILE_MD5_MISMATCH` 错误。响应包含 `X-File-MD5` 头，用于客户端完整性校验。

---

### 验证文件完整性

| 项目           | 详情                                                      |
| -------------- | --------------------------------------------------------- |
| **接口地址**   | `GET /api/common/files/{dateUrl}/{storedName}/verify`     |
| **权限**       | 需要 `FILE_READ` 权限                                      |

验证磁盘文件与数据库存储的 MD5 哈希是否匹配。重新计算物理文件的 MD5 并与数据库记录比较。

返回：`{ "match": true/false, "storedMd5": "..." }`

---

### 删除文件

| 项目           | 详情                                           |
| -------------- | ---------------------------------------------- |
| **接口地址**   | `DELETE /api/common/files/{dateUrl}/{storedName}` |
| **权限**       | 需要 `FILE_DELETE` 权限                        |

从磁盘删除文件并逻辑删除数据库记录。物理文件从文件系统中移除，数据库记录通过 `@TableLogic` 软删除（`deleted` 标志设为 1）。

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

## 組織 — 部門

### 部門詳細取得

| 項目               | 詳細                        |
| ------------------ | --------------------------- |
| **エンドポイント** | `GET /api/departments/{id}` |
| **認可**           | `DEPT_READ` 権限が必要      |

ID で単一の部門を取得します。`id`、`deptName`、`deptCode`、`parentId`（トップレベルは 0）、`leaderEmployeeId`、`sortOrder`、`status`、`version` を返します。見つからない場合は 404 例外をスローします。

---

### 部門ツリー取得

| 項目               | 詳細                        |
| ------------------ | --------------------------- |
| **エンドポイント** | `GET /api/departments/tree` |
| **認可**           | `DEPT_READ` 権限が必要      |

完全な部門階層をネストされたツリーとして返します。全部門を単一クエリで読み込み、メモリ上でネストされた `children` 配列に組み立てます。同階層は `sortOrder` の昇順（null は最後）、次に `id` の昇順で並べられます。孤立ノード（親が存在しないまたは論理削除済み）はトップレベルノードとして浮上し、可視性を保ちます。各ノードは詳細エンドポイントと同じフィールドに加えて `children` 配列を持ちます。

---

### 部門作成

| 項目               | 詳細                     |
| ------------------ | ------------------------ |
| **エンドポイント** | `POST /api/departments`  |
| **認可**           | `DEPT_WRITE` 権限が必要  |

新しい部門を作成します。挿入前に親部門の存在（トップレベル以外）と部門コードの一意性を検証します。`parentId` が null の場合は `0`（トップレベル）に正規化されます。作成された部門 ID を返します。

**リクエストボディ：**

- `deptName`（必須）：部門名、最大 64 文字
- `deptCode`（オプション）：部門コード、最大 64 文字（一意である必要があります）
- `parentId`（オプション）：親部門 ID、`0` または省略でトップレベル
- `leaderEmployeeId`（オプション）：部門長の従業員 ID
- `sortOrder`（オプション）：同階層内の並び順
- `status`（オプション）：ステータス（`0` 無効 / `1` 有効）

---

### 部門更新

| 項目               | 詳細                     |
| ------------------ | ------------------------ |
| **エンドポイント** | `PUT /api/departments`   |
| **認可**           | `DEPT_WRITE` 権限が必要  |

既存の部門を更新します。親部門の存在、コードの一意性（自身を除く）、および新しい親が循環を作らないこと（部門は自身またはその子孫の子になれません）を検証します。`version` フィールドによる楽観的ロックを使用します。

**リクエストボディ：**

- `id`（必須）：部門 ID
- `deptName`（必須）：部門名、最大 64 文字
- `deptCode`（オプション）：部門コード、最大 64 文字
- `parentId`（オプション）：親部門 ID（`0` でトップレベル）
- `leaderEmployeeId`（オプション）：部門長の従業員 ID
- `sortOrder`（オプション）：同階層内の並び順
- `status`（オプション）：ステータス（`0` 無効 / `1` 有効）
- `version`（必須）：楽観的ロック用バージョン

---

### 部門削除

| 項目               | 詳細                      |
| ------------------ | ------------------------- |
| **エンドポイント** | `DELETE /api/departments` |
| **認可**           | `DEPT_DELETE` 権限が必要  |

子部門がなく、割り当てられた従業員もないことを検証してから部門を削除します。明示的なバージョン条件による楽観的ロックを使用します。

**リクエストボディ：**

- `id`（必須）：部門 ID
- `version`（必須）：楽観的ロック用バージョン

---

## 組織 — 役職

### 役職詳細取得

| 項目               | 詳細                        |
| ------------------ | --------------------------- |
| **エンドポイント** | `GET /api/positions/{id}`   |
| **認可**           | `POSITION_READ` 権限が必要  |

ID で単一の役職を取得します。`id`、`positionName`、`positionCode`、`sortOrder`、`status`、`version` を返します。見つからない場合は 404 例外をスローします。

---

### 役職一覧取得

| 項目               | 詳細                                                |
| ------------------ | --------------------------------------------------- |
| **エンドポイント** | `GET /api/positions`                                |
| **認可**           | `POSITION_READ` 権限が必要                          |
| **パラメータ**     | `positionName`（部分一致）、`status`（完全一致）    |

オプション条件で役職を検索します。結果は `sortOrder` 昇順、次に `id` 昇順で並べられます。各項目は `id`、`positionName`、`positionCode`、`sortOrder`、`status`、`version` を含みます。

---

### 役職作成

| 項目               | 詳細                       |
| ------------------ | -------------------------- |
| **エンドポイント** | `POST /api/positions`      |
| **認可**           | `POSITION_WRITE` 権限が必要 |

新しい役職を作成します。挿入前に役職コードの一意性を検証します。作成された役職 ID を返します。

**リクエストボディ：**

- `positionName`（必須）：役職名、最大 64 文字
- `positionCode`（オプション）：役職コード、最大 64 文字（一意である必要があります）
- `sortOrder`（オプション）：並び順
- `status`（オプション）：ステータス（`0` 無効 / `1` 有効）

---

### 役職更新

| 項目               | 詳細                       |
| ------------------ | -------------------------- |
| **エンドポイント** | `PUT /api/positions`       |
| **認可**           | `POSITION_WRITE` 権限が必要 |

既存の役職を更新します。コードの一意性（自身を除く）を検証します。`version` フィールドによる楽観的ロックを使用します。

**リクエストボディ：**

- `id`（必須）：役職 ID
- `positionName`（必須）：役職名、最大 64 文字
- `positionCode`（オプション）：役職コード、最大 64 文字
- `sortOrder`（オプション）：並び順
- `status`（オプション）：ステータス（`0` 無効 / `1` 有効）
- `version`（必須）：楽観的ロック用バージョン

---

### 役職削除

| 項目               | 詳細                        |
| ------------------ | --------------------------- |
| **エンドポイント** | `DELETE /api/positions`     |
| **認可**           | `POSITION_DELETE` 権限が必要 |

役職がどの従業員にも割り当てられていないことを検証してから削除します。明示的なバージョン条件による楽観的ロックを使用します。

**リクエストボディ：**

- `id`（必須）：役職 ID
- `version`（必須）：楽観的ロック用バージョン

---

## 組織 — 従業員

### 従業員詳細取得

| 項目               | 詳細                       |
| ------------------ | -------------------------- |
| **エンドポイント** | `GET /api/employees/{id}`  |
| **認可**           | `EMPLOYEE_READ` 権限が必要 |

ID で単一の従業員を取得します。`id`、`userId`（連携ログインアカウント）、`employeeNo`、`realName`、`deptId`、`positionId`、`managerId`、`mobile`、`email`、`gender`、`idCardNo`、`birthday`、`hireDate`、`status`、`version` を返します。見つからない場合は 404 例外をスローします。

---

### 従業員ページング取得

| 項目               | 詳細                                                                                          |
| ------------------ | --------------------------------------------------------------------------------------------- |
| **エンドポイント** | `GET /api/employees/page`                                                                     |
| **認可**           | `EMPLOYEE_READ` 権限が必要                                                                    |
| **パラメータ**     | `page`、`size`、`realName`（部分一致）、`employeeNo`（部分一致）、`deptId`、`status`           |

ページング対応で従業員を検索します。ホワイトリストフィールド（`id`、`employee_no`、`real_name`、`dept_id`、`hire_date`、`create_at`、`update_at`）に対する `sortBy`/`sortOrder` による動的ソートをサポートし、デフォルトは `update_at` の降順です。各項目は `id`、`userId`、`employeeNo`、`realName`、`deptId`、`positionId`、`mobile`、`email`、`hireDate`、`status`、`version` を含みます。

---

### 従業員作成

| 項目               | 詳細                       |
| ------------------ | -------------------------- |
| **エンドポイント** | `POST /api/employees`      |
| **認可**           | `EMPLOYEE_WRITE` 権限が必要 |

単一トランザクション内で従業員**および連携ログインアカウント**を作成します。従業員番号の一意性を検証し、ログインアカウントをプロビジョニング（ランダムパスワード生成、アカウントを未アクティブに設定、コミット後にウェルカムメール送信、指定ロールを割り当て）してから、生成されたユーザー ID を従業員に連携します。いずれかが失敗した場合、両方がアトミックにロールバックされ、孤立した従業員やアカウントは残りません。

**リクエストボディ：**

- `username`（必須）：ログインユーザー名、英数字とアンダースコアのみ（`^[a-zA-Z0-9_]+$`）、最大 64 文字
- `employeeNo`（オプション）：従業員番号、最大 32 文字（一意である必要があります）
- `realName`（必須）：氏名、最大 64 文字
- `deptId`（オプション）：部門 ID
- `positionId`（オプション）：役職 ID
- `managerId`（オプション）：直属上司の従業員 ID
- `mobile`（オプション）：携帯電話番号、最大 20 文字
- `email`（必須）：メールアドレス、有効なメール形式、最大 128 文字（ログインアカウントに使用）
- `gender`（オプション）：性別（`0` 不明 / `1` 男性 / `2` 女性）
- `idCardNo`（オプション）：身分証番号、最大 32 文字
- `birthday`（オプション）：生年月日（`yyyy-MM-dd`）
- `hireDate`（オプション）：入社日（`yyyy-MM-dd`）
- `status`（オプション）：在職ステータス（`0` 退職 / `1` 在職 / `2` 休職）
- `roleIds`（オプション）：自動作成アカウントに割り当てるロール ID リスト

作成された従業員 ID を返します。

---

### 従業員更新

| 項目               | 詳細                       |
| ------------------ | -------------------------- |
| **エンドポイント** | `PUT /api/employees`       |
| **認可**           | `EMPLOYEE_WRITE` 権限が必要 |

従業員プロフィールのみを更新します。連携ログインアカウント（ユーザー名、パスワード、ロール）はユーザー管理モジュールで個別に管理されます。従業員番号の一意性（自身を除く）を検証します。`version` フィールドによる楽観的ロックを使用します。

**リクエストボディ：**

- `id`（必須）：従業員 ID
- `employeeNo`（オプション）：従業員番号、最大 32 文字
- `realName`（必須）：氏名、最大 64 文字
- `deptId`（オプション）：部門 ID
- `positionId`（オプション）：役職 ID
- `managerId`（オプション）：直属上司の従業員 ID
- `mobile`（オプション）：携帯電話番号、最大 20 文字
- `email`（オプション）：メールアドレス、有効なメール形式、最大 128 文字
- `gender`（オプション）：性別（`0` 不明 / `1` 男性 / `2` 女性）
- `idCardNo`（オプション）：身分証番号、最大 32 文字
- `birthday`（オプション）：生年月日（`yyyy-MM-dd`）
- `hireDate`（オプション）：入社日（`yyyy-MM-dd`）
- `status`（オプション）：在職ステータス（`0` 退職 / `1` 在職 / `2` 休職）
- `version`（必須）：楽観的ロック用バージョン

---

### 従業員削除

| 項目               | 詳細                        |
| ------------------ | --------------------------- |
| **エンドポイント** | `DELETE /api/employees`     |
| **認可**           | `EMPLOYEE_DELETE` 権限が必要 |

明示的なバージョン条件による楽観的ロックを使用して従業員プロフィールを削除します。連携ログインアカウントは意図的に変更されず、ユーザー管理モジュールで管理する必要があります。

**リクエストボディ：**

- `id`（必須）：従業員 ID
- `version`（必須）：楽観的ロック用バージョン

---

## ワークフロー — 休暇承認

> 承認フローは Flowable BPMN エンジンによって駆動されます。業務データは `oa_leave_request`（MyBatis-Plus）に保存され、Flowable がプロセスインスタンスと承認タスクを管理します。承認者は組織テーブルから動的に解決され、そのログインユーザー名がタスク担当者として Flowable に渡されます。
>
> **承認段階（日数しきい値による分岐）：**
>
> - `days < 3` —— 1 段階：直属上司のみ（`mst_employee.manager_id`）。
> - `days >= 3` —— 2 段階：直属上司 → 部門責任者（`mst_department.leader_employee_id`）。1 段階目の承認後も業務ステータスは `承認待ち` のままで、プロセスが実際に終了するまで継続します。
>
> **年次有給休暇（`leaveType = 1`）の付与ルール：** 付与は入社記念日ごとに行われ、付与日時点の勤続年数で段階分けされます（1〜3 年 → 5 日、4〜6 年 → 7 日、7 年以上 → 9 日）。各付与バッチは付与日から 24 か月間有効で、FIFO（最も早く付与されたものから）で消費されます。提出時に年次有給休暇は同一トランザクション内で消費され、却下・取り下げ時には元のバッチへ正確に返還されます。その他の休暇種別は残数を検証・消費しません。

### 休暇申請の提出

| 項目               | 詳細                     |
| ------------------ | ------------------------ |
| **エンドポイント** | `POST /api/leaves`       |
| **認可**           | `LEAVE_APPLY` 権限が必要 |

単一トランザクション内で休暇申請を提出し、承認プロセスを開始します。申請者は認証済みユーザーから解決され、直属上司は申請者の `managerId` から解決されます。`days >= 3` の場合は部門責任者も解決されます（申請者の部門に責任者が設定されている必要があります）。年次有給休暇は残数を検証し FIFO で消費します。作成された休暇申請 ID を返します。

**検証と失敗：**

- `leaveType` は定義済みの種別である必要があります。そうでない場合は `error.leave.invalid_type`。
- `days` は `0.5` の正の倍数である必要があります。そうでない場合は `error.leave.days_not_half_unit`。
- 直属上司がいない → `error.leave.no_manager`。
- `days >= 3` だが部門に責任者がいない → `error.leave.no_dept_leader`。
- 年次有給休暇が残数を超過 → `error.leave.insufficient_annual`。

操作全体（残数消費 + レコード挿入 + プロセス開始）は同一トランザクション内で行われ、いずれかが失敗するとすべてロールバックされます。

**リクエストボディ：**

- `leaveType`（必須）：休暇種別（`1` 年次 / `2` 病気 / `3` 私用 / `4` 結婚 / `5` 出産 / `9` その他）、最大 2 文字
- `startTime`（必須）：休暇開始時刻（`yyyy-MM-dd'T'HH:mm:ss`）
- `endTime`（必須）：休暇終了時刻（`yyyy-MM-dd'T'HH:mm:ss`）
- `days`（必須）：休暇日数、`0.5` の正の倍数である必要があります
- `reason`（オプション）：休暇理由、最大 500 文字

---

### 自分の保留中タスク取得

| 項目               | 詳細                       |
| ------------------ | -------------------------- |
| **エンドポイント** | `GET /api/leaves/tasks`    |
| **認可**           | `LEAVE_APPROVE` 権限が必要 |

現在のユーザーに割り当てられた承認待ちタスクを一覧表示します。担当者が現在のユーザー名と一致するタスクを Flowable から検索し、各タスクの基となる休暇申請の業務データを読み込みます。各項目は `leaveId`、`employeeId`、`leaveType`、`startTime`、`endTime`、`days`、`reason`、`createAt` を含みます。

---

### 休暇申請の審査

| 項目               | 詳細                       |
| ------------------ | -------------------------- |
| **エンドポイント** | `POST /api/leaves/review`  |
| **認可**           | `LEAVE_APPROVE` 権限が必要 |

休暇申請を承認または却下します。現在のユーザーは承認待ち Flowable タスクの担当者である必要があります。`approved` 変数でタスクを完了し（BPMN 排他ゲートウェイを駆動）、業務ステータスを同期して承認コメントを永続化します。

- **承認、1 段階（`days < 3`）** → プロセス終了 → ステータス `1` 承認。
- **承認、2 段階のうち 1 段階目（`days >= 3`）** → プロセスは部門責任者へ進む → ステータスは `0` 承認待ちのまま（コメントは記録）。
- **承認、最終段階** → プロセス終了 → ステータス `1` 承認。
- **却下（任意の段階）** → プロセス終了 → ステータス `2` 却下。年次有給休暇は消費した残数が返還されます。

休暇申請が承認待ち状態でない場合、または承認待ちタスクがない場合は失敗します。

**リクエストボディ：**

- `id`（必須）：休暇申請 ID
- `approved`（必須）：承認判断（`true` 承認、`false` 却下）
- `comment`（オプション）：承認コメント、最大 500 文字

---

### 休暇申請の取り下げ

| 項目               | 詳細                        |
| ------------------ | --------------------------- |
| **エンドポイント** | `POST /api/leaves/withdraw` |
| **認可**           | `LEAVE_APPLY` 権限が必要    |

承認待ちの休暇申請を取り下げます。申請者本人のみが自分の申請を取り下げることができ、承認待ち状態の間のみ可能です。Flowable プロセスインスタンスを削除し、業務レコードを取り下げ済み（`3`）としてマークします。年次有給休暇は元のバッチへ正確に返還されます。呼び出し元が本人でない場合、または申請が承認待ちでない場合は失敗します。

**リクエストボディ：**

- `id`（必須）：休暇申請 ID

---

### 自分の年次有給休暇残数取得

| 項目               | 詳細                             |
| ------------------ | -------------------------------- |
| **エンドポイント** | `GET /api/leaves/annual-balance` |
| **認可**           | `LEAVE_APPLY` 権限が必要         |

現在のユーザーの本日時点の年次有給休暇残数を返します。初回アクセス時に未失効の付与予定バッチを物理化（遅延付与）し、合計利用可能日数と各バッチの内訳を返します。返却内容：

- `employeeId` —— 現在のユーザーの従業員 ID
- `totalAvailable` —— 利用可能な年次有給休暇の合計日数
- `batches[]` —— 有効（未失効）な付与バッチ。付与日の昇順で、各項目に `grantDate`、`expireDate`、`grantedDays`、`usedDays`、`remainingDays` を含む

---

### 従業員別の年次有給休暇残数取得

| 項目               | 詳細                                          |
| ------------------ | --------------------------------------------- |
| **エンドポイント** | `GET /api/leaves/annual-balance/{employeeId}` |
| **認可**           | `LEAVE_READ` 権限が必要                       |

他の従業員の年次有給休暇残数を参照する特権ビューです。レスポンス構造は「自分」エンドポイントと同じです。従業員が見つからない場合は 404 例外をスローします。

---

### 休暇申請詳細取得

| 項目               | 詳細                     |
| ------------------ | ------------------------ |
| **エンドポイント** | `GET /api/leaves/{id}`   |
| **認可**           | `LEAVE_READ` 権限が必要  |

ID で単一の休暇申請を取得します。`id`、`employeeId`、`leaveType`、`startTime`、`endTime`、`days`、`reason`、`status`（`0` 承認待ち / `1` 承認 / `2` 却下 / `3` 取り下げ）、`processInstanceId`、`approverId`、`approvalComment`、`version` を返します。見つからない場合は 404 例外をスローします。

---

### 休暇申請ページング取得

| 項目               | 詳細                                                  |
| ------------------ | ----------------------------------------------------- |
| **エンドポイント** | `GET /api/leaves/page`                                |
| **認可**           | `LEAVE_READ` 権限が必要                               |
| **パラメータ**     | `page`、`size`、`employeeId`、`leaveType`、`status`   |

ページング対応で休暇申請を検索します。ホワイトリストフィールド（`id`、`employee_id`、`leave_type`、`status`、`create_at`、`update_at`）に対する `sortBy`/`sortOrder` による動的ソートをサポートし、デフォルトは `create_at` の降順です。各項目は `id`、`employeeId`、`leaveType`、`startTime`、`endTime`、`days`、`status`、`approverId`、`createAt`、`version` を含みます。

---

## 共通

### 列挙リスト取得

| 項目               | 詳細                                            |
| ------------------ | ----------------------------------------------- |
| **エンドポイント** | `GET /api/common/enums`                         |
| **認可**           | 公開（認証不要）                                |
| **パラメータ**     | `type` — サポート値：`user-status`、`role-code`、`module`、`sub-module`、`leave-type` |

タイプ別に列挙データリストを返します。`user-status`（ユーザーステータスオプション）、`role-code`（ロールコードオプション）、`module`（モジュールオプション）、`sub-module`（サブモジュールオプション）、`leave-type`（休暇タイプオプション）をサポートします。各項目には `code` と `name` フィールドが含まれます。

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

### ファイルアップロード

| 項目               | 詳細                                                                                                  |
| ------------------ | ----------------------------------------------------------------------------------------------------- |
| **エンドポイント** | `POST /api/common/files`                                                                              |
| **認可**           | `FILE_WRITE` 権限が必要                                                                              |
| **パラメータ**     | `file`（必須）— マルチパートファイル、`associateType`（任意）— 関連タイプ、`associateId`（任意）— 関連エンティティ ID |
| **Content-Type**   | `multipart/form-data`                                                                                |

単一ファイルをサーバーにアップロードします。ファイルは日付ベースのサブディレクトリ（`yyyy/MM/dd`）に UUID ベースのファイル名で保存され、競合とパストラバーサル攻撃を防止します。アップロード中に `DigestInputStream` で MD5 ハッシュを計算し、整合性検証のためデータベースに保存します。ファイルタイプは拡張子から自動検出されます（1=画像、2=ドキュメント、3=アーカイブ、9=その他）。拡張子は `app.upload.allowed-extensions` で設定された許可リストに含まれている必要があります。ファイルサイズは `app.upload.max-file-size-mb` を超えることはできません。

戻り値：`id`、`storedName`、`originalFilename`、`datePath`、`dateUrl`、`size`、`contentType`、`md5`。

---

### ファイル一括アップロード

| 項目               | 詳細                                                                                              |
| ------------------ | ------------------------------------------------------------------------------------------------- |
| **エンドポイント** | `POST /api/common/files/batch`                                                                   |
| **認可**           | `FILE_WRITE` 権限が必要                                                                          |
| **パラメータ**     | `files`（必須）— マルチパートファイル配列、`associateType`（任意）、`associateId`（任意）       |
| **Content-Type**   | `multipart/form-data`                                                                            |

複数ファイルを一括アップロードします。各ファイルは単一アップロードと同じルールで独立して処理されます。ファイル総数は `app.upload.max-files-per-request` を超えることはできません。

戻り値：`id`、`storedName`、`originalFilename`、`datePath`、`dateUrl`、`size`、`contentType`、`md5` の配列。

---

### ファイルダウンロード

| 項目               | 詳細                                                    |
| ------------------ | ------------------------------------------------------- |
| **エンドポイント** | `GET /api/common/files/{dateUrl}/{storedName}`           |
| **認可**           | `FILE_READ` 権限が必要                                  |
| **レスポンス**     | `Content-Disposition: attachment` ヘッダー付きのバイナリファイル |

日付 URL と保存名でファイルをダウンロードします。サーバーは返却前に MD5 整合性を検証します。ディスク上のファイルが保存された MD5 ハッシュと一致しない場合、`FILE_MD5_MISMATCH` エラーが返されます。レスポンスには `X-File-MD5` ヘッダーが含まれ、クライアント側で整合性検証に使用できます。

---

### ファイル整合性検証

| 項目               | 詳細                                                          |
| ------------------ | ------------------------------------------------------------- |
| **エンドポイント** | `GET /api/common/files/{dateUrl}/{storedName}/verify`         |
| **認可**           | `FILE_READ` 権限が必要                                        |

ディスク上のファイルがデータベースに保存された MD5 ハッシュと一致するか検証します。物理ファイルの MD5 を再計算し、データベースレコードと比較します。

戻り値：`{ "match": true/false, "storedMd5": "..." }`

---

### ファイル削除

| 項目               | 詳細                                                |
| ------------------ | --------------------------------------------------- |
| **エンドポイント** | `DELETE /api/common/files/{dateUrl}/{storedName}`   |
| **認可**           | `FILE_DELETE` 権限が必要                            |

ディスクからファイルを削除し、データベースレコードを論理削除します。物理ファイルはファイルシステムから削除され、データベースレコードは `@TableLogic` により論理削除（`deleted` フラグを 1 に設定）されます。

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
