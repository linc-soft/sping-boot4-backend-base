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
INSERT INTO mst_user (username, password, status, create_by, update_by)
VALUES ('admin',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
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
       (2, 'Role Viewer', 'ROLE_VIEW', 'Can view roles', 'system', 'system'),
       (3, 'Role Creator', 'ROLE_NEW', 'Can create new roles', 'system', 'system'),
       (4, 'Role Editor', 'ROLE_EDIT', 'Can edit roles', 'system', 'system'),
       (5, 'Role Deleter', 'ROLE_DEL', 'Can delete roles', 'system', 'system'),
       (6, 'User Viewer', 'USER_VIEW', 'Can view users', 'system', 'system'),
       (7, 'User Creator', 'USER_NEW', 'Can create new users', 'system', 'system'),
       (8, 'User Editor', 'USER_EDIT', 'Can edit users', 'system', 'system'),
       (9, 'User Deleter', 'USER_DEL', 'Can delete users', 'system', 'system');

-- ============================================================
-- Initial Data: role inheritance relationship
-- ============================================================
INSERT INTO mst_role_inheritance (child_role_id, parent_role_id, create_by, update_by)
VALUES (1, 2, 'system', 'system'),
       (1, 3, 'system', 'system'),
       (1, 4, 'system', 'system'),
       (1, 5, 'system', 'system'),
       (1, 6, 'system', 'system'),
       (1, 7, 'system', 'system'),
       (1, 8, 'system', 'system'),
       (1, 9, 'system', 'system');

-- ============================================================
-- Initial Data: user-role relationship
-- ============================================================
INSERT INTO mst_user_role (user_id, role_id, create_by, update_by)
VALUES (1, 1, 'system', 'system');
