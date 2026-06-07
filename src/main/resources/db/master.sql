-- ============================================================
-- Database Initialization Script
-- Description: Master tables DDL
--  * mst_user
--  * mst_role
--  * mst_user_role
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
  create_by VARCHAR(20) COMMENT 'Creator',
  create_at DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by VARCHAR(20) COMMENT 'Updater',
  update_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version   INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted   TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_user_username (username),
  INDEX idx_user_email (email)
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
       (2, 'Role Reader', 'ROLE_READ', 'Can read roles', 'system', 'system'),
       (3, 'Role Writer', 'ROLE_WRITE', 'Can write roles', 'system', 'system'),
       (4, 'Role Deleter', 'ROLE_DELETE', 'Can delete roles', 'system', 'system'),
       (5, 'Role Exporter', 'ROLE_EXPORT', 'Can export roles', 'system', 'system'),
       (6, 'User Reader', 'USER_READ', 'Can read users', 'system', 'system'),
       (7, 'User Writer', 'USER_WRITE', 'Can write users', 'system', 'system'),
       (8, 'User Deleter', 'USER_DELETE', 'Can delete users', 'system', 'system'),
       (9, 'User Exporter', 'USER_EXPORT', 'Can export users', 'system', 'system'),
       (10, 'Log Reader', 'LOG_READ', 'Can read logs', 'system', 'system'),
       (11, 'Log Exporter', 'LOG_EXPORT', 'Can export logs', 'system', 'system');

-- ============================================================
-- Initial Data: user-role relationship
-- ============================================================
INSERT INTO mst_user_role (user_id, role_id, create_by, update_by)
VALUES (1, 1, 'system', 'system');
