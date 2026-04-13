-- ============================================================
-- Monthly Partition Schema
-- Recommended for: High-volume logging (100k+ rows/day)
--
-- Notes:
--   1. Partition key create_time must be part of the primary key (MySQL requirement)
--   2. Only p_init + p_future partitions are created initially
--   3. After table creation, you MUST run the stored procedures in
--      partition-maintenance.sql and call sp_create_monthly_partitions
--      to generate future partitions
--
-- Deployment steps:
--   1. Execute this file (create tables)
--   2. Execute partition-maintenance.sql (create stored procedures)
--   3. Call stored procedure to create future partitions
--      (replace 'your_schema' with actual database name):
--        CALL sp_create_monthly_partitions('sys_access_log', 'your_schema', 6);
--        CALL sp_create_monthly_partitions('sys_error_log', 'your_schema', 6);
--        CALL sp_create_monthly_partitions('sys_operation_log', 'your_schema', 6);
--   4. Enable Event Scheduler tasks (see bottom of partition-maintenance.sql)
--
-- Partition behavior:
--   RANGE partition means "less than boundary value", e.g.:
--     p_init: VALUES LESS THAN (TO_DAYS('2026-05-01'))
--     -> Accepts all data before 2026-05-01 (including historical data)
--   p_future is the catch-all partition for data beyond existing ranges
-- ============================================================
-- ============================================================
-- sys_access_log table (monthly partition)
-- Records access logs for all HTTP requests
-- ============================================================
DROP TABLE IF EXISTS sys_access_log;

CREATE TABLE IF NOT EXISTS sys_access_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace ID',
    request_method VARCHAR(10) DEFAULT NULL COMMENT 'Request method (GET/POST/PUT/DELETE)',
    request_url VARCHAR(255) DEFAULT NULL COMMENT 'Request URL',
    request_params TEXT DEFAULT NULL COMMENT 'Request parameters (JSON format)',
    request_headers TEXT DEFAULT NULL COMMENT 'Request headers (JSON format)',
    request_body TEXT DEFAULT NULL COMMENT 'Request body (JSON format)',
    response_status INT DEFAULT NULL COMMENT 'Response status code',
    response_headers TEXT DEFAULT NULL COMMENT 'Response headers (JSON format)',
    response_body TEXT DEFAULT NULL COMMENT 'Response body (JSON format)',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT 'Client IP address',
    user_agent VARCHAR(255) DEFAULT NULL COMMENT 'User-Agent',
    username VARCHAR(64) DEFAULT NULL COMMENT 'Operating username',
    duration BIGINT DEFAULT NULL COMMENT 'Processing time (milliseconds)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    PRIMARY KEY (id, create_time),
    KEY idx_access_log_trace_id (trace_id),
    KEY idx_access_log_create_time (create_time),
    KEY idx_access_log_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Access log (monthly partition)'
PARTITION BY
    RANGE (TO_DAYS (create_time)) (
        PARTITION p_init
        VALUES
            LESS THAN (TO_DAYS ('2026-05-01')) COMMENT 'Initial partition (includes historical data)',
            PARTITION p_future
        VALUES
            LESS THAN MAXVALUE COMMENT 'Catch-all partition'
    );

-- ============================================================
-- sys_error_log table (monthly partition)
-- Records exception details
-- ============================================================
DROP TABLE IF EXISTS sys_error_log;

CREATE TABLE IF NOT EXISTS sys_error_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace ID',
    exception_file VARCHAR(64) DEFAULT NULL COMMENT 'Exception file name',
    exception_class VARCHAR(64) DEFAULT NULL COMMENT 'Exception class name',
    exception_method VARCHAR(64) DEFAULT NULL COMMENT 'Exception method name',
    exception_line INT DEFAULT NULL COMMENT 'Exception line number',
    exception_message TEXT DEFAULT NULL COMMENT 'Exception message',
    root_cause_message TEXT DEFAULT NULL COMMENT 'Root cause message',
    stack_trace TEXT DEFAULT NULL COMMENT 'Stack trace',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    PRIMARY KEY (id, create_time),
    KEY idx_error_log_trace_id (trace_id),
    KEY idx_error_log_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Error log (monthly partition)'
PARTITION BY
    RANGE (TO_DAYS (create_time)) (
        PARTITION p_init
        VALUES
            LESS THAN (TO_DAYS ('2026-05-01')) COMMENT 'Initial partition (includes historical data)',
            PARTITION p_future
        VALUES
            LESS THAN MAXVALUE COMMENT 'Catch-all partition'
    );

-- ============================================================
-- sys_operation_log table (monthly partition)
-- Records business operation logs for Service layer methods
-- annotated with @OperationLog
-- ============================================================
DROP TABLE IF EXISTS sys_operation_log;

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace ID',
    module VARCHAR(64) DEFAULT NULL COMMENT 'Primary module',
    sub_module VARCHAR(64) DEFAULT NULL COMMENT 'Secondary module',
    operation_type VARCHAR(10) DEFAULT NULL COMMENT 'Operation type (CREATE/UPDATE/DELETE/QUERY)',
    description VARCHAR(255) DEFAULT NULL COMMENT 'Operation description',
    duration BIGINT DEFAULT NULL COMMENT 'Operation processing time (milliseconds)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    PRIMARY KEY (id, create_time),
    KEY idx_operation_log_trace_id (trace_id),
    KEY idx_operation_log_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Operation log (monthly partition)'
PARTITION BY
    RANGE (TO_DAYS (create_time)) (
        PARTITION p_init
        VALUES
            LESS THAN (TO_DAYS ('2026-05-01')) COMMENT 'Initial partition (includes historical data)',
            PARTITION p_future
        VALUES
            LESS THAN MAXVALUE COMMENT 'Catch-all partition'
    );