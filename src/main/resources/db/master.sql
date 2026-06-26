-- ============================================================
-- Database Initialization Script
-- Description: Master tables DDL
--  * mst_user
--  * mst_role
--  * mst_user_role
--  * mst_role_inheritance
--  * mst_department
--  * mst_position
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
  email     VARCHAR(128) DEFAULT NULL COMMENT 'Email address',
  status    VARCHAR(1)   DEFAULT NULL COMMENT 'Status',
  real_name VARCHAR(64)  DEFAULT NULL COMMENT 'Real name',
  dept_id   BIGINT       DEFAULT NULL COMMENT 'Department ID (mst_department.id)',
  position_id BIGINT     DEFAULT NULL COMMENT 'Position ID (mst_position.id)',
  mobile    VARCHAR(20)  DEFAULT NULL COMMENT 'Mobile phone',
  gender    VARCHAR(1)   DEFAULT NULL COMMENT 'Gender (0 unknown / 1 male / 2 female)',
  birthday  DATE         DEFAULT NULL COMMENT 'Date of birth',
  create_by VARCHAR(20) COMMENT 'Creator',
  create_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by VARCHAR(20) COMMENT 'Updater',
  update_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version   INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted   TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_user_username (username),
  INDEX idx_user_email (email),
  INDEX idx_user_dept_id (dept_id),
  INDEX idx_user_position_id (position_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'User table';

-- ============================================================
-- mst_role table
-- Records role information such as role name, role code, etc.
-- Note: role_code is only used for base roles, custom roles may have NULL role_code
-- ============================================================
DROP TABLE IF EXISTS mst_role;
CREATE TABLE IF NOT EXISTS mst_role
(
  id          BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  role_name   VARCHAR(64)  DEFAULT NULL COMMENT 'Role name',
  role_code   VARCHAR(64)  DEFAULT NULL COMMENT 'Role code (only for base roles)',
  description VARCHAR(255) DEFAULT NULL COMMENT 'Description',
  create_by   VARCHAR(20) COMMENT 'Creator',
  create_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by   VARCHAR(20) COMMENT 'Updater',
  update_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version     INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted     TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id)
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
-- mst_role_inheritance table
-- Records the inheritance relationship between roles (multi-inheritance).
-- A child role inherits all permissions from its parent roles.
-- Note: No UNIQUE KEY due to logical delete (deleted flag).
--       Duplicate prevention is handled at the application layer.
-- ============================================================
DROP TABLE IF EXISTS mst_role_inheritance;
CREATE TABLE IF NOT EXISTS mst_role_inheritance
(
  id             BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  child_role_id  BIGINT     DEFAULT NULL COMMENT 'Child role ID (the role that inherits)',
  parent_role_id BIGINT     DEFAULT NULL COMMENT 'Parent role ID (the role being inherited)',
  create_by      VARCHAR(20) COMMENT 'Creator',
  create_at      DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by      VARCHAR(20) COMMENT 'Updater',
  update_at      DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  deleted        TINYINT(1) DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_child_role_id (child_role_id),
  INDEX idx_parent_role_id (parent_role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Role inheritance relationship table';

-- ============================================================
-- mst_department table
-- Multi-level department tree. parent_id = 0 for top-level departments.
-- leader_user_id references mst_user.id (department head),
-- used by approval workflow for routing.
-- ============================================================
DROP TABLE IF EXISTS mst_department;
CREATE TABLE IF NOT EXISTS mst_department
(
  id                 BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  dept_name          VARCHAR(64)  DEFAULT NULL COMMENT 'Department name',
  dept_code          VARCHAR(64)  DEFAULT NULL COMMENT 'Department code (business identifier)',
  parent_id          BIGINT       DEFAULT 0 COMMENT 'Parent department ID (0 = top level)',
  leader_user_id     BIGINT       DEFAULT NULL COMMENT 'Department head (mst_user.id)',
  sort_order         INT          DEFAULT 0 COMMENT 'Sort order among siblings',
  status             VARCHAR(1)   DEFAULT '1' COMMENT 'Status (0 disabled / 1 enabled)',
  create_by          VARCHAR(20) COMMENT 'Creator',
  create_at          DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by          VARCHAR(20) COMMENT 'Updater',
  update_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version            INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted            TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_dept_parent_id (parent_id),
  INDEX idx_dept_code (dept_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Department table';

-- ============================================================
-- mst_position table
-- Job positions / titles. Referenced by mst_user.position_id.
-- ============================================================
DROP TABLE IF EXISTS mst_position;
CREATE TABLE IF NOT EXISTS mst_position
(
  id            BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  position_name VARCHAR(64)  DEFAULT NULL COMMENT 'Position name',
  position_code VARCHAR(64)  DEFAULT NULL COMMENT 'Position code (business identifier)',
  sort_order    INT          DEFAULT 0 COMMENT 'Sort order',
  status        VARCHAR(1)   DEFAULT '1' COMMENT 'Status (0 disabled / 1 enabled)',
  create_by     VARCHAR(20) COMMENT 'Creator',
  create_at     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by     VARCHAR(20) COMMENT 'Updater',
  update_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version       INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted       TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_position_code (position_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Position table';

-- ============================================================
-- Initial Data: admin user
-- Default password: admin123 (BCrypt encoded)
-- ============================================================
INSERT INTO mst_user (username, password, email, status, create_by, update_by)
VALUES ('admin',
        '$2a$10$EnO/OowYMyVUUaYMHwaR.Oh.tnaLtj8Y/zhhlgL3L/qDkuUc.Hr9C',
        'admin@example.com',
        '1',
        'system',
        'system');

-- ============================================================
-- Initial Data: admin role, role manager, user manager
-- ============================================================
INSERT INTO mst_role (id,
                      role_name,
                      role_code,
                      description,
                      create_by,
                      update_by)
VALUES (1, 'Administrator', 'ADMIN', 'Administrator', 'system', 'system'),
       (2, 'List Options', 'LIST_OPTIONS', 'Can list select options', 'system', 'system'),
       (3, 'List Role', 'LIST_ROLE', 'Can list roles', 'system', 'system'),
       (4, 'View Role', 'VIEW_ROLE', 'Can view role details', 'system', 'system'),
       (5, 'Create Role', 'CREATE_ROLE', 'Can create roles', 'system', 'system'),
       (6, 'Update Role', 'UPDATE_ROLE', 'Can update roles', 'system', 'system'),
       (7, 'Delete Role', 'DELETE_ROLE', 'Can delete roles', 'system', 'system'),
       (8, 'List User', 'LIST_USER', 'Can list users', 'system', 'system'),
       (9, 'View User', 'VIEW_USER', 'Can view user details', 'system', 'system'),
       (10, 'Create User', 'CREATE_USER', 'Can create users', 'system', 'system'),
       (11, 'Update User', 'UPDATE_USER', 'Can update users', 'system', 'system'),
       (12, 'Delete User', 'DELETE_USER', 'Can delete users', 'system', 'system'),
       (13, 'Export User', 'EXPORT_USER', 'Can export users', 'system', 'system'),
       (14, 'List Department', 'LIST_DEPARTMENT', 'Can list departments', 'system', 'system'),
       (15, 'View Department', 'VIEW_DEPARTMENT', 'Can view department details', 'system', 'system'),
       (16, 'Create Department', 'CREATE_DEPARTMENT', 'Can create departments', 'system', 'system'),
       (17, 'Update Department', 'UPDATE_DEPARTMENT', 'Can update departments', 'system', 'system'),
       (18, 'Delete Department', 'DELETE_DEPARTMENT', 'Can delete departments', 'system', 'system'),
       (19, 'List Position', 'LIST_POSITION', 'Can list positions', 'system', 'system'),
       (20, 'View Position', 'VIEW_POSITION', 'Can view position details', 'system', 'system'),
       (21, 'Create Position', 'CREATE_POSITION', 'Can create positions', 'system', 'system'),
       (22, 'Update Position', 'UPDATE_POSITION', 'Can update positions', 'system', 'system'),
       (23, 'Delete Position', 'DELETE_POSITION', 'Can delete positions', 'system', 'system'),
       (24, 'List Resource', 'LIST_RESOURCE', 'Can list resources', 'system', 'system'),
       (25, 'View Resource', 'VIEW_RESOURCE', 'Can view resource details', 'system', 'system'),
       (26, 'Update Resource', 'UPDATE_RESOURCE', 'Can update resources', 'system', 'system'),
       (27, 'List Log', 'LIST_LOG', 'Can list logs', 'system', 'system'),
       (28, 'View Log', 'VIEW_LOG', 'Can view log details', 'system', 'system'),
       (29, 'Export Log', 'EXPORT_LOG', 'Can export logs', 'system', 'system'),
       (30, 'Upload File', 'UPLOAD_FILE', 'Can upload files', 'system', 'system'),
       (31, 'Download File', 'DOWNLOAD_FILE', 'Can download files', 'system', 'system'),
       (32, 'Remove File', 'REMOVE_FILE', 'Can remove files', 'system', 'system');

-- ============================================================
-- Initial Data: user-role relationship
-- ============================================================
INSERT INTO mst_user_role (user_id, role_id, create_by, update_by)
VALUES (1, 1, 'system', 'system');
