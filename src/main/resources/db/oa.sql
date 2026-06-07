-- ============================================================
-- Database Initialization Script
-- Description: OA (Office Automation) organization tables DDL
--  * mst_department  (multi-level department tree)
--  * mst_position    (job position / title)
--  * mst_employee    (employee profile, linked to mst_user)
-- Engine: InnoDB
-- Character Set: utf8mb4
-- ============================================================

-- ============================================================
-- mst_department table
-- Multi-level department tree. parent_id = 0 for top-level departments.
-- leader_employee_id references mst_employee.id (department head),
-- used by approval workflow for routing.
-- ============================================================
DROP TABLE IF EXISTS mst_department;
CREATE TABLE IF NOT EXISTS mst_department
(
  id                 BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  dept_name          VARCHAR(64)  DEFAULT NULL COMMENT 'Department name',
  dept_code          VARCHAR(64)  DEFAULT NULL COMMENT 'Department code (business identifier)',
  parent_id          BIGINT       DEFAULT 0 COMMENT 'Parent department ID (0 = top level)',
  leader_employee_id BIGINT       DEFAULT NULL COMMENT 'Department head (mst_employee.id)',
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
-- Job positions / titles. Referenced by mst_employee.position_id.
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
-- mst_employee table
-- Employee profile. Links to mst_user (login account, nullable),
-- mst_department, mst_position, and self-references manager_id
-- (direct supervisor, used by approval workflow for hierarchical routing).
-- ============================================================
DROP TABLE IF EXISTS mst_employee;
CREATE TABLE IF NOT EXISTS mst_employee
(
  id          BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id     BIGINT       DEFAULT NULL COMMENT 'Login account ID (mst_user.id), nullable',
  employee_no VARCHAR(32)  DEFAULT NULL COMMENT 'Employee number (business identifier)',
  real_name   VARCHAR(64)  DEFAULT NULL COMMENT 'Real name',
  dept_id     BIGINT       DEFAULT NULL COMMENT 'Department ID (mst_department.id)',
  position_id BIGINT       DEFAULT NULL COMMENT 'Position ID (mst_position.id)',
  manager_id  BIGINT       DEFAULT NULL COMMENT 'Direct supervisor (mst_employee.id)',
  mobile      VARCHAR(20)  DEFAULT NULL COMMENT 'Mobile phone',
  email       VARCHAR(128) DEFAULT NULL COMMENT 'Email address',
  gender      VARCHAR(1)   DEFAULT NULL COMMENT 'Gender (0 unknown / 1 male / 2 female)',
  id_card_no  VARCHAR(32)  DEFAULT NULL COMMENT 'ID card number',
  birthday    DATE         DEFAULT NULL COMMENT 'Date of birth',
  hire_date   DATE         DEFAULT NULL COMMENT 'Hire date',
  status      VARCHAR(1)   DEFAULT '1' COMMENT 'Employment status (0 left / 1 active / 2 on leave)',
  create_by   VARCHAR(20) COMMENT 'Creator',
  create_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by   VARCHAR(20) COMMENT 'Updater',
  update_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version     INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted     TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_emp_user_id (user_id),
  INDEX idx_emp_dept_id (dept_id),
  INDEX idx_emp_manager_id (manager_id),
  INDEX idx_emp_employee_no (employee_no)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Employee table';

-- ============================================================
-- oa_leave_request table
-- Leave request business data. Flowable owns the approval flow;
-- this table stores the business fields and links to the BPMN
-- process instance via process_instance_id.
-- ============================================================
DROP TABLE IF EXISTS oa_leave_request;
CREATE TABLE IF NOT EXISTS oa_leave_request
(
  id                  BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  employee_id         BIGINT       DEFAULT NULL COMMENT 'Applicant employee ID (mst_employee.id)',
  leave_type          VARCHAR(2)   DEFAULT NULL COMMENT 'Leave type (1 annual / 2 sick / 3 personal / 4 marriage / 5 maternity / 9 other)',
  start_time          DATETIME     DEFAULT NULL COMMENT 'Leave start time',
  end_time            DATETIME     DEFAULT NULL COMMENT 'Leave end time',
  days                DECIMAL(5,1) DEFAULT NULL COMMENT 'Number of leave days',
  reason              VARCHAR(500) DEFAULT NULL COMMENT 'Leave reason',
  status              VARCHAR(2)   DEFAULT NULL COMMENT 'Status (0 pending / 1 approved / 2 rejected / 3 withdrawn)',
  process_instance_id VARCHAR(64)  DEFAULT NULL COMMENT 'Flowable process instance ID',
  approver_id         BIGINT       DEFAULT NULL COMMENT 'Resolved approver employee ID (direct manager)',
  approval_comment    VARCHAR(500) DEFAULT NULL COMMENT 'Approver comment',
  create_by           VARCHAR(20) COMMENT 'Creator',
  create_at           DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by           VARCHAR(20) COMMENT 'Updater',
  update_at           DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version             INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted             TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_leave_employee_id (employee_id),
  INDEX idx_leave_status (status),
  INDEX idx_leave_process_instance_id (process_instance_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Leave request table';

-- ============================================================
-- oa_annual_leave_grant table
-- Annual-leave quota, one row per grant batch. Quota is granted on
-- each employment anniversary (lazy-written on first access). A batch
-- is valid for 24 months from its grant_date; consumption is FIFO
-- (earliest grant_date first). granted_days is locked at grant time
-- based on tenure tier (5 / 7 / 9 days).
-- ============================================================
DROP TABLE IF EXISTS oa_annual_leave_grant;
CREATE TABLE IF NOT EXISTS oa_annual_leave_grant
(
  id           BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  employee_id  BIGINT       DEFAULT NULL COMMENT 'Employee ID (mst_employee.id)',
  grant_date   DATE         DEFAULT NULL COMMENT 'Grant date (employment anniversary)',
  expire_date  DATE         DEFAULT NULL COMMENT 'Expiry date (grant_date + 24 months)',
  granted_days DECIMAL(5,1) DEFAULT NULL COMMENT 'Days granted in this batch (tenure tier 5/7/9)',
  used_days    DECIMAL(5,1) DEFAULT 0.0 COMMENT 'Days consumed from this batch',
  create_by    VARCHAR(20) COMMENT 'Creator',
  create_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by    VARCHAR(20) COMMENT 'Updater',
  update_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  version      INT          DEFAULT 0 COMMENT 'Optimistic lock version',
  deleted      TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_grant_employee_id (employee_id),
  INDEX idx_grant_expire_date (expire_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Annual leave grant table';

-- ============================================================
-- oa_annual_leave_consumption table
-- Per-batch consumption ledger for a leave request. When an annual
-- leave request is submitted, FIFO consumption across grant batches
-- writes one row per affected batch (leave_request_id + grant_id +
-- days). On reject/withdraw, refund restores each batch by exactly
-- the recorded days.
-- ============================================================
DROP TABLE IF EXISTS oa_annual_leave_consumption;
CREATE TABLE IF NOT EXISTS oa_annual_leave_consumption
(
  id               BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  leave_request_id BIGINT       DEFAULT NULL COMMENT 'Leave request ID (oa_leave_request.id)',
  grant_id         BIGINT       DEFAULT NULL COMMENT 'Grant batch ID (oa_annual_leave_grant.id)',
  days             DECIMAL(5,1) DEFAULT NULL COMMENT 'Days consumed from this batch',
  create_by        VARCHAR(20) COMMENT 'Creator',
  create_at        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  update_by        VARCHAR(20) COMMENT 'Updater',
  update_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  deleted          TINYINT(1)   DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  INDEX idx_consumption_leave_request_id (leave_request_id),
  INDEX idx_consumption_grant_id (grant_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Annual leave consumption ledger table';

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
       (17, 'Position Deleter', 'POSITION_DELETE', 'Can delete positions', 'system', 'system'),
       (18, 'Employee Reader', 'EMPLOYEE_READ', 'Can read employees', 'system', 'system'),
       (19, 'Employee Writer', 'EMPLOYEE_WRITE', 'Can write employees', 'system', 'system'),
       (20, 'Employee Deleter', 'EMPLOYEE_DELETE', 'Can delete employees', 'system', 'system'),
       (21, 'Employee Exporter', 'EMPLOYEE_EXPORT', 'Can export employees', 'system', 'system'),
       (22, 'Leave Applicant', 'LEAVE_APPLY', 'Can submit leave requests', 'system', 'system'),
       (23, 'Leave Reader', 'LEAVE_READ', 'Can read leave requests', 'system', 'system'),
       (24, 'Leave Approver', 'LEAVE_APPROVE', 'Can approve leave requests', 'system', 'system');
