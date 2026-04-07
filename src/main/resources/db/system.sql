-- ============================================================
-- Database Initialization Script
-- Description: System tables DDL
--  * sys_access_log
--  * sys_error_log
--  * sys_operation_log
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
  username         VARCHAR(64) DEFAULT NULL COMMENT 'Operating username',
  duration         BIGINT       DEFAULT NULL COMMENT 'Processing time (milliseconds)',
  create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_access_trace_id (trace_id),
  KEY idx_access_create_at (create_time),
  KEY idx_access_username (username)
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
  exception_file     VARCHAR(64) DEFAULT NULL COMMENT 'Exception file name',
  exception_class    VARCHAR(64) DEFAULT NULL COMMENT 'Exception class name',
  exception_method   VARCHAR(64) DEFAULT NULL COMMENT 'Exception method name',
  exception_line     INT          DEFAULT NULL COMMENT 'Exception line number',
  exception_message  TEXT         DEFAULT NULL COMMENT 'Exception message',
  root_cause_message TEXT         DEFAULT NULL COMMENT 'Root cause message',
  stack_trace        TEXT         DEFAULT NULL COMMENT 'Stack trace',
  create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_error_trace_id (trace_id),
  KEY idx_error_create_at (create_time)
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
  id              BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  trace_id        VARCHAR(64)  DEFAULT NULL COMMENT 'Trace ID',
  module          VARCHAR(64) DEFAULT NULL COMMENT 'Primary module',
  sub_module      VARCHAR(64) DEFAULT NULL COMMENT 'Secondary module',
  operation_type  VARCHAR(10)  DEFAULT NULL COMMENT 'Operation type (CREATE/UPDATE/DELETE/QUERY)',
  description     VARCHAR(255) DEFAULT NULL COMMENT 'Operation description',
  duration        BIGINT       DEFAULT NULL COMMENT 'Operation processing time (milliseconds)',
  create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_operation_trace_id (trace_id),
  KEY idx_operation_create_at (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Operation log';
