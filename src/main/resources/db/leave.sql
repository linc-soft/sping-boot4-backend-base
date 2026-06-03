-- ============================================================
-- Database Initialization Script
-- Description: Leave management tables DDL
--  * mst_employee     - Employee information (extends mst_user)
--  * txn_leave        - Leave requests
--  * txn_annual_leave - Annual leave balances
-- Engine: InnoDB
-- Character Set: utf8mb4
-- ============================================================

-- ============================================================
-- mst_employee table
-- Employee business information extending mst_user (1:1 relationship)
-- ============================================================
CREATE TABLE IF NOT EXISTS mst_employee
(
  id         BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id    BIGINT       NOT NULL COMMENT 'FK → mst_user.id',
  nickname   VARCHAR(30)  NOT NULL DEFAULT '' COMMENT 'Display name',
  remark     VARCHAR(500)          DEFAULT NULL COMMENT 'Remark',
  mobile     VARCHAR(11)           DEFAULT '' COMMENT 'Mobile phone',
  sex        TINYINT               DEFAULT 0 COMMENT 'Gender (0=Male, 1=Female)',
  hired_date DATE                  DEFAULT NULL COMMENT 'Hire date',
  create_by  VARCHAR(20)                    COMMENT 'Creator',
  create_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  update_by  VARCHAR(20)                    COMMENT 'Updater',
  update_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_employee_user_id (user_id),
  INDEX idx_employee_hired_date (hired_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Employee information';

-- ============================================================
-- txn_leave table
-- Leave requests with approval workflow
-- ============================================================
CREATE TABLE IF NOT EXISTS txn_leave
(
  id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id         BIGINT        NOT NULL COMMENT 'FK → mst_employee.user_id → mst_user.id',
  start_date      DATE          NOT NULL COMMENT 'Leave start date',
  end_date        DATE          NOT NULL COMMENT 'Leave end date',
  leave_type      INT           NOT NULL COMMENT 'Leave type (0=Annual,1=Personal,2=Sick,3=Marriage,4=Paternity,5=Bereavement)',
  duration        DECIMAL(5, 1) NOT NULL COMMENT 'Leave duration in days (multiple of 0.5)',
  reason          VARCHAR(256)  NOT NULL COMMENT 'Leave reason',
  status          INT           NOT NULL DEFAULT 0 COMMENT 'Status (0=Applying,1=Approved,2=Rejected)',
  approver_id     BIGINT                 DEFAULT NULL COMMENT 'Approver FK → mst_user.id',
  approve_time    DATETIME               DEFAULT NULL COMMENT 'Approval time',
  approve_reason  VARCHAR(256)           DEFAULT NULL COMMENT 'Approval/rejection reason',
  create_by       VARCHAR(20)                    COMMENT 'Creator',
  create_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  update_by       VARCHAR(20)                    COMMENT 'Updater',
  update_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted         TINYINT(1)    NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_leave_user_id (user_id),
  INDEX idx_leave_status (status),
  INDEX idx_leave_start_date (start_date),
  INDEX idx_leave_user_date (user_id, start_date, end_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Leave request';

-- ============================================================
-- txn_annual_leave table
-- Annual leave balance per user per year
-- ============================================================
CREATE TABLE IF NOT EXISTS txn_annual_leave
(
  id                                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id                            BIGINT        NOT NULL COMMENT 'FK → mst_employee.user_id → mst_user.id',
  year                               INT           NOT NULL COMMENT 'Calendar year',
  last_remaining_annual_leave_days   DECIMAL(5, 1) NOT NULL DEFAULT 0 COMMENT 'Carry-over from previous year',
  pre_effective_annual_leave_days   DECIMAL(5, 1) NOT NULL DEFAULT 0 COMMENT 'Days before anniversary date',
  effective_date                     DATE          NOT NULL COMMENT 'Anniversary date in this year',
  post_effective_annual_leave_days   DECIMAL(5, 1) NOT NULL DEFAULT 0 COMMENT 'Days after anniversary date',
  create_by                          VARCHAR(20)                    COMMENT 'Creator',
  create_at                          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  update_by                          VARCHAR(20)                    COMMENT 'Updater',
  update_at                          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted                            TINYINT(1)    NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_annual_leave_user_year (user_id, year)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Annual leave balance';

-- ============================================================
-- Seed data: Leave-related roles
-- ============================================================
INSERT INTO mst_role (role_name, role_code, description, create_by, update_by)
VALUES
  ('Leave Reader', 'LEAVE_READ', 'View leave requests', 'system', 'system'),
  ('Leave Applicant', 'LEAVE_WRITE', 'Submit leave requests', 'system', 'system'),
  ('Leave Approver', 'LEAVE_APPROVE', 'Approve or reject leave requests', 'system', 'system'),
  ('Leave Deleter', 'LEAVE_DELETE', 'Delete leave requests', 'system', 'system'),
  ('Regulations Writer', 'REGULATIONS_WRITE', 'Upload and manage regulation documents', 'system', 'system'),
  ('Employee Reader', 'EMPLOYEE_READ', 'View employee information', 'system', 'system'),
  ('Employee Writer', 'EMPLOYEE_WRITE', 'Create and edit employee information', 'system', 'system');