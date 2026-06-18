-- ============================================================
-- Database Initialization Script
-- Description: System tables DDL
--  * sys_access_log
--  * sys_error_log
--  * sys_operation_log
--  * sys_sql_log
--  * sys_file_upload
-- Engine: InnoDB
-- Character Set: utf8mb4
-- ============================================================
-- ============================================================
-- sys_access_log table
-- Records access logs for all HTTP requests
-- ============================================================
DROP TABLE IF EXISTS sys_access_log;

CREATE TABLE IF NOT EXISTS sys_access_log
(
  id               BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  trace_id         VARCHAR(64)  DEFAULT NULL COMMENT 'Trace ID',
  request_method   VARCHAR(10)  DEFAULT NULL COMMENT 'Request method (GET/POST/PUT/DELETE)',
  request_url      VARCHAR(255) DEFAULT NULL COMMENT 'Request URL',
  request_params   TEXT         DEFAULT NULL COMMENT 'Request parameters (JSON format)',
  request_headers  TEXT         DEFAULT NULL COMMENT 'Request headers (JSON format)',
  request_body     TEXT         DEFAULT NULL COMMENT 'Request body (JSON format)',
  response_status  INT          DEFAULT NULL COMMENT 'Response status code',
  response_headers TEXT         DEFAULT NULL COMMENT 'Response headers (JSON format)',
  response_body    TEXT         DEFAULT NULL COMMENT 'Response body (JSON format)',
  client_ip        VARCHAR(64)  DEFAULT NULL COMMENT 'Client IP address',
  user_agent       VARCHAR(255) DEFAULT NULL COMMENT 'User-Agent',
  username         VARCHAR(64)  DEFAULT NULL COMMENT 'Operating username',
  duration         BIGINT       DEFAULT NULL COMMENT 'Processing time (milliseconds)',
  create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_access_log_trace_id (trace_id),
  KEY idx_access_log_create_time (create_time),
  KEY idx_access_log_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Access log';

-- ============================================================
-- sys_error_log table
-- Records exception details
-- ============================================================
DROP TABLE IF EXISTS sys_error_log;

CREATE TABLE IF NOT EXISTS sys_error_log
(
  id                 BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  trace_id           VARCHAR(64)  DEFAULT NULL COMMENT 'Trace ID',
  exception_file     VARCHAR(64)  DEFAULT NULL COMMENT 'Exception file name',
  exception_class    VARCHAR(255) DEFAULT NULL COMMENT 'Exception class name',
  exception_method   VARCHAR(64)  DEFAULT NULL COMMENT 'Exception method name',
  exception_line     INT          DEFAULT NULL COMMENT 'Exception line number',
  exception_message  TEXT         DEFAULT NULL COMMENT 'Exception message',
  root_cause_message TEXT         DEFAULT NULL COMMENT 'Root cause message',
  stack_trace        TEXT         DEFAULT NULL COMMENT 'Stack trace',
  request_method     VARCHAR(10)  DEFAULT NULL COMMENT 'Request method (GET/POST/PUT/DELETE)',
  request_url        VARCHAR(255) DEFAULT NULL COMMENT 'Request URL',
  client_ip          VARCHAR(64)  DEFAULT NULL COMMENT 'Client IP address',
  username           VARCHAR(64)  DEFAULT NULL COMMENT 'Operating username',
  create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_error_log_trace_id (trace_id),
  KEY idx_error_log_create_time (create_time),
  KEY idx_error_log_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Error log';

-- ============================================================
-- sys_operation_log table
-- Records business operation logs for Service layer methods
-- annotated with @OperationLog
-- ============================================================
DROP TABLE IF EXISTS sys_operation_log;

CREATE TABLE IF NOT EXISTS sys_operation_log
(
  id             BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  trace_id       VARCHAR(64)  DEFAULT NULL COMMENT 'Trace ID',
  module         VARCHAR(64)  DEFAULT NULL COMMENT 'Primary module',
  sub_module     VARCHAR(64)  DEFAULT NULL COMMENT 'Secondary module',
  operation_type VARCHAR(10)  DEFAULT NULL COMMENT 'Operation type (CREATE/UPDATE/DELETE/QUERY)',
  description    VARCHAR(255) DEFAULT NULL COMMENT 'Operation description',
  duration       BIGINT       DEFAULT NULL COMMENT 'Operation processing time (milliseconds)',
  request_method VARCHAR(10)  DEFAULT NULL COMMENT 'Request method (GET/POST/PUT/DELETE)',
  request_url    VARCHAR(255) DEFAULT NULL COMMENT 'Request URL',
  client_ip      VARCHAR(64)  DEFAULT NULL COMMENT 'Client IP address',
  username       VARCHAR(64)  DEFAULT NULL COMMENT 'Operating username',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_operation_log_trace_id (trace_id),
  KEY idx_operation_log_create_time (create_time),
  KEY idx_operation_log_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Operation log';

-- ============================================================
-- sys_sql_log table
-- Records SQL execution logs for MyBatis mapper methods
-- ============================================================
DROP TABLE IF EXISTS sys_sql_log;

CREATE TABLE IF NOT EXISTS sys_sql_log
(
  id             BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  trace_id       VARCHAR(64)  DEFAULT NULL COMMENT 'Trace ID',
  sql_type       VARCHAR(10)  DEFAULT NULL COMMENT 'SQL type (SELECT/INSERT/UPDATE/DELETE)',
  sql_text       TEXT         DEFAULT NULL COMMENT 'SQL text',
  sql_params     TEXT         DEFAULT NULL COMMENT 'SQL parameters (JSON format)',
  duration       BIGINT       DEFAULT NULL COMMENT 'Execution duration (milliseconds)',
  mapper_class   VARCHAR(255) DEFAULT NULL COMMENT 'Mapper class name',
  mapper_method  VARCHAR(64)  DEFAULT NULL COMMENT 'Mapper method name',
  request_url    VARCHAR(255) DEFAULT NULL COMMENT 'Request URL',
  request_method VARCHAR(10)  DEFAULT NULL COMMENT 'Request method (GET/POST/PUT/DELETE)',
  client_ip      VARCHAR(64)  DEFAULT NULL COMMENT 'Client IP address',
  row_count      BIGINT       DEFAULT NULL COMMENT 'Affected row count',
  username       VARCHAR(64)  DEFAULT NULL COMMENT 'Operating username',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_sql_log_trace_id (trace_id),
  KEY idx_sql_log_create_time (create_time),
  KEY idx_sql_log_username (username),
  KEY idx_sql_log_sql_type (sql_type),
  KEY idx_sql_log_duration (duration)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SQL execution log';

-- ============================================================
-- sys_resource table
-- Frontend page/button permission resources (tree structure).
--  type: 0=directory, 1=page, 2=button
--  role_code: single value; NULL for directories (visibility via children).
--             Pages/buttons check against user's resolved role_codes.
--  resource_code: business code (e.g. user:read), unique.
--  resource_name: i18n key resolved on the frontend.
--  route_path: only for type=1 pages; NULL for directories/buttons.
-- ============================================================
DROP TABLE IF EXISTS sys_resource;
CREATE TABLE IF NOT EXISTS sys_resource
(
  id            BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  resource_code VARCHAR(128) NOT NULL COMMENT 'Business code (unique, e.g. user:read)',
  resource_name VARCHAR(128) NOT NULL COMMENT 'i18n key for display name',
  type          TINYINT      NOT NULL COMMENT '0=directory, 1=page, 2=button',
  parent_id     BIGINT       NOT NULL DEFAULT 0 COMMENT 'Parent resource ID (0 = top level)',
  route_path    VARCHAR(255) DEFAULT NULL COMMENT 'Route path (type=1 only)',
  icon          VARCHAR(64)  DEFAULT NULL COMMENT 'Menu icon (mdi-*)',
  sort_order    INT          DEFAULT 0 COMMENT 'Sort order among siblings',
  role_code     VARCHAR(64)  DEFAULT NULL COMMENT 'Role code for visibility (NULL for directories)',
  status        VARCHAR(1)   DEFAULT '1' COMMENT 'Status (0 disabled / 1 enabled)',
  create_by     VARCHAR(20) COMMENT 'Creator',
  create_at     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by     VARCHAR(20) COMMENT 'Updater',
  update_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version       INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted       TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_code (resource_code),
  KEY idx_resource_parent_id (parent_id),
  KEY idx_resource_type (type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Frontend permission resource';

-- ============================================================
-- sys_resource seed data
-- ============================================================
-- Directories (type=0, role_code always NULL)
INSERT INTO sys_resource (id, resource_code, resource_name, type, parent_id, route_path, icon, sort_order, role_code, status, create_by, update_by)
VALUES
  (1, 'dir:master', 'nav.master', 0, 0, NULL, 'mdi-database-outline', 10, NULL, '1', 'system', 'system'),
  (2, 'dir:logs', 'nav.logs', 0, 0, NULL, 'mdi-file-document-outline', 20, NULL, '1', 'system', 'system');

-- Pages (type=1)
INSERT INTO sys_resource (id, resource_code, resource_name, type, parent_id, route_path, icon, sort_order, role_code, status, create_by, update_by)
VALUES
  (10, 'home', 'nav.home', 1, 0, '/', 'mdi-home', 1, NULL, '1', 'system', 'system'),
  (11, 'user:read', 'nav.users', 1, 1, '/master/users', 'mdi-account-group', 11, 'USER_READ', '1', 'system', 'system'),
  (12, 'role:read', 'nav.roles', 1, 1, '/master/roles', 'mdi-shield-account-outline', 12, 'ROLE_READ', '1', 'system', 'system'),
  (13, 'dept:read', 'nav.departments', 1, 1, '/master/departments', 'mdi-office-building-outline', 13, 'DEPT_READ', '1', 'system', 'system'),
  (14, 'position:read', 'nav.positions', 1, 1, '/master/positions', 'mdi-badge-account-horizontal-outline', 14, 'POSITION_READ', '1', 'system', 'system'),
  (21, 'log:access:read', 'nav.accessLogs', 1, 2, '/logs', 'mdi-web', 21, 'LOG_READ', '1', 'system', 'system'),
  (22, 'log:error:read', 'nav.errorLogs', 1, 2, '/logs/error', 'mdi-alert-circle-outline', 22, 'LOG_READ', '1', 'system', 'system'),
  (23, 'log:operation:read', 'nav.operationLogs', 1, 2, '/logs/operation', 'mdi-cog-outline', 23, 'LOG_READ', '1', 'system', 'system'),
  (24, 'log:sql:read', 'nav.sqlLogs', 1, 2, '/logs/sql', 'mdi-database-search-outline', 24, 'LOG_READ', '1', 'system', 'system'),
  (25, 'log:trace:read', 'log.nav.trace', 1, 2, '/logs/trace/:traceId', NULL, 25, 'LOG_READ', '1', 'system', 'system');

INSERT INTO sys_resource (id, resource_code, resource_name, type, parent_id, route_path, icon, sort_order, role_code, status, create_by, update_by)
VALUES
  (26, 'resource:read', 'nav.resources', 1, 1, '/master/resources', 'mdi-view-module-outline', 15, 'RESOURCE_READ', '1', 'system', 'system');

-- Buttons (type=2)
INSERT INTO sys_resource (id, resource_code, resource_name, type, parent_id, route_path, icon, sort_order, role_code, status, create_by, update_by)
VALUES
  -- User page buttons
  (101, 'user:create', 'resource.user.create', 2, 11, NULL, NULL, 1, 'USER_WRITE', '1', 'system', 'system'),
  (102, 'user:update', 'resource.user.update', 2, 11, NULL, NULL, 2, 'USER_WRITE', '1', 'system', 'system'),
  (103, 'user:delete', 'resource.user.delete', 2, 11, NULL, NULL, 3, 'USER_DELETE', '1', 'system', 'system'),
  (104, 'user:export', 'resource.user.export', 2, 11, NULL, NULL, 4, 'USER_EXPORT', '1', 'system', 'system'),
  -- Role page buttons
  (111, 'role:create', 'resource.role.create', 2, 12, NULL, NULL, 1, 'ROLE_WRITE', '1', 'system', 'system'),
  (112, 'role:update', 'resource.role.update', 2, 12, NULL, NULL, 2, 'ROLE_WRITE', '1', 'system', 'system'),
  (113, 'role:delete', 'resource.role.delete', 2, 12, NULL, NULL, 3, 'ROLE_DELETE', '1', 'system', 'system'),
  (114, 'role:export', 'resource.role.export', 2, 12, NULL, NULL, 4, 'ROLE_EXPORT', '1', 'system', 'system'),
  -- Department page buttons
  (121, 'dept:create', 'resource.dept.create', 2, 13, NULL, NULL, 1, 'DEPT_WRITE', '1', 'system', 'system'),
  (122, 'dept:update', 'resource.dept.update', 2, 13, NULL, NULL, 2, 'DEPT_WRITE', '1', 'system', 'system'),
  (123, 'dept:delete', 'resource.dept.delete', 2, 13, NULL, NULL, 3, 'DEPT_DELETE', '1', 'system', 'system'),
  -- Position page buttons
  (131, 'position:create', 'resource.position.create', 2, 14, NULL, NULL, 1, 'POSITION_WRITE', '1', 'system', 'system'),
  (132, 'position:update', 'resource.position.update', 2, 14, NULL, NULL, 2, 'POSITION_WRITE', '1', 'system', 'system'),
  (133, 'position:delete', 'resource.position.delete', 2, 14, NULL, NULL, 3, 'POSITION_DELETE', '1', 'system', 'system'),
  -- Log page buttons (export)
  (211, 'log:access:export', 'resource.log.export', 2, 21, NULL, NULL, 1, 'LOG_EXPORT', '1', 'system', 'system'),
  (212, 'log:error:export', 'resource.log.export', 2, 22, NULL, NULL, 1, 'LOG_EXPORT', '1', 'system', 'system'),
  (213, 'log:operation:export', 'resource.log.export', 2, 23, NULL, NULL, 1, 'LOG_EXPORT', '1', 'system', 'system'),
  (214, 'log:sql:export', 'resource.log.export', 2, 24, NULL, NULL, 1, 'LOG_EXPORT', '1', 'system', 'system');

INSERT INTO sys_resource (id, resource_code, resource_name, type, parent_id, route_path, icon, sort_order, role_code, status, create_by, update_by)
VALUES
  (141, 'resource:update', 'resourceManagement.actions.edit', 2, 26, NULL, NULL, 1, 'RESOURCE_WRITE', '1', 'system', 'system');

-- ============================================================
-- sys_file_upload table
-- Records metadata of uploaded files
-- ============================================================
DROP TABLE IF EXISTS sys_file_upload;

CREATE TABLE IF NOT EXISTS sys_file_upload
(
  id                BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  file_type         INT          DEFAULT NULL COMMENT 'File type (1=image, 2=document, 3=archive, 9=other)',
  stored_name       VARCHAR(128) DEFAULT NULL COMMENT 'UUID-based storage filename',
  original_filename VARCHAR(255) DEFAULT NULL COMMENT 'Original filename from client',
  extension         VARCHAR(32)  DEFAULT NULL COMMENT 'File extension (lowercase, without dot)',
  file_size         BIGINT       DEFAULT NULL COMMENT 'File size in bytes',
  content_type      VARCHAR(128) DEFAULT NULL COMMENT 'MIME type',
  file_path         VARCHAR(512) DEFAULT NULL COMMENT 'Full relative path from upload directory',
  date_path         VARCHAR(32)  DEFAULT NULL COMMENT 'Date-based subdirectory (e.g., "2026/06/03")',
  date_url          VARCHAR(32)  DEFAULT NULL COMMENT 'Date in URL-friendly format (e.g., "2026-06-03")',
  associate_type    VARCHAR(64)  DEFAULT NULL COMMENT 'Association type (e.g., USER_AVATAR, TICKET_ATTACHMENT)',
  associate_id      BIGINT       DEFAULT NULL COMMENT 'Associated business entity ID',
  md5               VARCHAR(32)  DEFAULT NULL COMMENT 'MD5 hash of file content',
  creator           VARCHAR(64)  DEFAULT NULL COMMENT 'Record creation user',
  create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  deleted           TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  KEY idx_file_upload_associate (associate_type, associate_id),
  KEY idx_file_upload_create_time (create_time),
  KEY idx_file_upload_creator (creator)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'File upload';
