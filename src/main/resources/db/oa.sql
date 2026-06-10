-- ============================================================
-- Database Initialization Script
-- Description: OA (Office Automation) organization tables DDL
--  * mst_department  (multi-level department tree)
--  * mst_position    (job position / title)
-- Engine: InnoDB
-- Character Set: utf8mb4
-- ============================================================

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
-- Initial Data: permission roles for organization modules
-- Follows existing convention: role_code IS the granted authority.
-- (Continues the id sequence after master.sql which ends at id=11)
-- ============================================================
INSERT INTO mst_role (id, role_name, role_code, description, create_by, update_by)
VALUES (12, 'Department Reader', 'DEPT_READ', 'Can read departments', 'system', 'system'),
       (13, 'Department Writer', 'DEPT_WRITE', 'Can write departments', 'system', 'system'),
       (14, 'Department Deleter', 'DEPT_DELETE', 'Can delete departments', 'system', 'system'),
       (15, 'Position Reader', 'POSITION_READ', 'Can read positions', 'system', 'system'),
       (16, 'Position Writer', 'POSITION_WRITE', 'Can write positions', 'system', 'system'),
       (17, 'Position Deleter', 'POSITION_DELETE', 'Can delete positions', 'system', 'system');
