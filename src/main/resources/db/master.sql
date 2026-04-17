-- ============================================================
-- Database Initialization Script
-- Description: Master tables DDL
--  * mst_user
--  * mst_role
--  * mst_user_role
--  * mst_dept
--  * mst_user_dept
--  * mst_role_data_scope
--  * mst_data_permission_grant
-- Engine: InnoDB
-- Character Set: utf8mb4
-- ============================================================
-- ============================================================
-- mst_user table
-- Records user information such as username, password, etc.
-- ============================================================
DROP TABLE IF EXISTS mst_user;
CREATE TABLE IF NOT EXISTS mst_user
(
  id        BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  username  VARCHAR(64)  DEFAULT NULL COMMENT 'Username',
  password  VARCHAR(128) DEFAULT NULL COMMENT 'Password',
  status    VARCHAR(1)   DEFAULT NULL COMMENT 'Status',
  create_by VARCHAR(20) COMMENT 'Creator',
  create_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by VARCHAR(20) COMMENT 'Updater',
  update_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version   INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted   TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'User table';

-- ============================================================
-- mst_role table
-- Records role information such as role name, role code, etc.
-- ============================================================
DROP TABLE IF EXISTS mst_role;
CREATE TABLE IF NOT EXISTS mst_role
(
  id          BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  role_name   VARCHAR(64)  DEFAULT NULL COMMENT 'Role name',
  role_code   VARCHAR(64)  DEFAULT NULL COMMENT 'Role code',
  description VARCHAR(255) DEFAULT NULL COMMENT 'Description',
  create_by   VARCHAR(20) COMMENT 'Creator',
  create_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by   VARCHAR(20) COMMENT 'Updater',
  update_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version     INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted     TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_role_code (role_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Role table';

-- ============================================================
-- mst_user_role table
-- Records the relationship between users and roles
-- ============================================================
DROP TABLE IF EXISTS mst_user_role;
CREATE TABLE IF NOT EXISTS mst_user_role
(
  id        BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id   BIGINT     DEFAULT NULL COMMENT 'User ID',
  role_id   BIGINT     DEFAULT NULL COMMENT 'Role ID',
  create_by VARCHAR(20) COMMENT 'Creator',
  create_at DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by VARCHAR(20) COMMENT 'Updater',
  update_at DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  deleted   TINYINT(1) DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'User-role relationship table';

-- ============================================================
-- ALTER mst_role: add parent_role_id for single inheritance
-- ============================================================
ALTER TABLE mst_role
  ADD COLUMN IF NOT EXISTS parent_role_id BIGINT DEFAULT NULL COMMENT 'Parent role ID for single inheritance (NULL = root role)',
  ADD INDEX IF NOT EXISTS idx_role_parent_role_id (parent_role_id);

-- ============================================================
-- mst_dept table
-- Stores the department tree structure used as the base
-- dimension for data permission control.
-- ============================================================
CREATE TABLE IF NOT EXISTS mst_dept
(
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  dept_name   VARCHAR(64)  NOT NULL                COMMENT 'Department name',
  parent_id   BIGINT       DEFAULT NULL            COMMENT 'Parent department ID (NULL = root)',
  sort_order  INT          DEFAULT 0               COMMENT 'Sort order within same level',
  status      VARCHAR(1)   DEFAULT '1'             COMMENT 'Status: 1=active, 0=disabled',
  create_by   VARCHAR(20)                          COMMENT 'Creator',
  create_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  update_by   VARCHAR(20)                          COMMENT 'Updater',
  update_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  version     INT          NOT NULL DEFAULT 0      COMMENT 'Optimistic lock version',
  deleted     TINYINT(1)   DEFAULT 0               COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_dept_parent_id (parent_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Department tree table';

-- ============================================================
-- mst_user_dept table
-- Records the many-to-many relationship between users and
-- departments.
-- ============================================================
CREATE TABLE IF NOT EXISTS mst_user_dept
(
  id        BIGINT     NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id   BIGINT     NOT NULL                COMMENT 'User ID',
  dept_id   BIGINT     NOT NULL                COMMENT 'Department ID',
  create_by VARCHAR(20)                        COMMENT 'Creator',
  create_at DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  update_by VARCHAR(20)                        COMMENT 'Updater',
  update_at DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted   TINYINT(1) DEFAULT 0               COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_user_dept_user_id (user_id),
  INDEX idx_user_dept_dept_id (dept_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'User-department relationship table';

-- ============================================================
-- mst_role_data_scope table
-- Configures the data scope (department-based) for each role.
-- scope_type: ALL / DEPT / DEPT_AND_CHILD
-- perm_bits:  bitmask READ=1, WRITE=2, DELETE=4, EXPORT=8
-- ============================================================
CREATE TABLE IF NOT EXISTS mst_role_data_scope
(
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  role_id     BIGINT      NOT NULL                COMMENT 'Role ID',
  scope_type  VARCHAR(20) NOT NULL                COMMENT 'Scope type: ALL / DEPT / DEPT_AND_CHILD',
  dept_id     BIGINT      DEFAULT NULL            COMMENT 'Target dept ID (NULL when scope_type=ALL)',
  perm_bits   TINYINT     NOT NULL DEFAULT 0      COMMENT 'Permission bitmask: READ=1,WRITE=2,DELETE=4,EXPORT=8',
  enabled     TINYINT(1)  DEFAULT 1               COMMENT 'Enabled flag: 1=active, 0=disabled',
  inheritable TINYINT(1)  DEFAULT 1               COMMENT 'Whether child roles can inherit this scope',
  create_by   VARCHAR(20)                         COMMENT 'Creator',
  create_at   DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  update_by   VARCHAR(20)                         COMMENT 'Updater',
  update_at   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  version     INT         NOT NULL DEFAULT 0      COMMENT 'Optimistic lock version',
  deleted     TINYINT(1)  DEFAULT 0               COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_rds_role_id (role_id),
  INDEX idx_rds_dept_id (dept_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Role data scope configuration table';

-- ============================================================
-- mst_data_permission_grant table
-- Stores row-level permission grants for specific resources.
-- subject_type: USER / ROLE / DEPT
-- perm_bits:    bitmask READ=1, WRITE=2, DELETE=4, EXPORT=8
-- ============================================================
CREATE TABLE IF NOT EXISTS mst_data_permission_grant
(
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  resource_type VARCHAR(64) NOT NULL                COMMENT 'Resource type enum value (e.g. ORDER, CUSTOMER)',
  resource_id   BIGINT      NOT NULL                COMMENT 'Resource ID',
  subject_type  VARCHAR(10) NOT NULL                COMMENT 'Subject type: USER / ROLE / DEPT',
  subject_id    BIGINT      NOT NULL                COMMENT 'Subject ID (user_id / role_id / dept_id)',
  perm_bits     TINYINT     NOT NULL DEFAULT 0      COMMENT 'Permission bitmask: READ=1,WRITE=2,DELETE=4,EXPORT=8',
  inheritable   TINYINT(1)  DEFAULT 1               COMMENT 'Whether permission propagates up the dept/role tree',
  valid_from    DATETIME    DEFAULT NULL             COMMENT 'Valid from (NULL = immediately)',
  valid_until   DATETIME    DEFAULT NULL             COMMENT 'Valid until (NULL = no expiry)',
  create_by     VARCHAR(20)                         COMMENT 'Creator',
  create_at     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  update_by     VARCHAR(20)                         COMMENT 'Updater',
  update_at     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  version       INT         NOT NULL DEFAULT 0      COMMENT 'Optimistic lock version',
  deleted       TINYINT(1)  DEFAULT 0               COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_dpg_resource (resource_type, resource_id),
  INDEX idx_dpg_subject (subject_type, subject_id),
  INDEX idx_dpg_valid_until (valid_until)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Row-level data permission grant table';

-- ============================================================
-- Initial Data: admin user
-- Default password: admin123 (BCrypt encoded)
-- ============================================================
INSERT INTO mst_user (username, password, status, create_by, update_by)
VALUES ('admin',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
        '1',
        'system',
        'system');

INSERT INTO mst_role (role_name,
                      role_code,
                      description,
                      create_by,
                      update_by)
VALUES ('Administrator',
        'ADMIN',
        'Administrator',
        'system',
        'system');

INSERT INTO mst_user_role (user_id, role_id, create_by, update_by)
VALUES (1, 1, 'system', 'system');
