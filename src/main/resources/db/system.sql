-- ============================================================
-- Database Initialization Script
-- Description: System tables DDL
--  * sys_access_log
--  * sys_error_log
--  * sys_operation_log
--  * sys_file_upload
-- Engine: InnoDB
-- Character Set: utf8mb4
-- ============================================================
-- ============================================================
-- sys_access_log table
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
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_access_log_trace_id (trace_id),
  KEY idx_access_log_create_time (create_time),
  KEY idx_access_log_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Access log';

-- ============================================================
-- sys_error_log table
-- Records exception details
-- ============================================================
DROP TABLE IF EXISTS sys_error_log;

CREATE TABLE IF NOT EXISTS sys_error_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace ID',
  exception_file VARCHAR(64) DEFAULT NULL COMMENT 'Exception file name',
  exception_class VARCHAR(255) DEFAULT NULL COMMENT 'Exception class name',
  exception_method VARCHAR(64) DEFAULT NULL COMMENT 'Exception method name',
  exception_line INT DEFAULT NULL COMMENT 'Exception line number',
  exception_message TEXT DEFAULT NULL COMMENT 'Exception message',
  root_cause_message TEXT DEFAULT NULL COMMENT 'Root cause message',
  stack_trace TEXT DEFAULT NULL COMMENT 'Stack trace',
  request_method VARCHAR(10) DEFAULT NULL COMMENT 'Request method (GET/POST/PUT/DELETE)',
  request_url VARCHAR(255) DEFAULT NULL COMMENT 'Request URL',
  client_ip VARCHAR(64) DEFAULT NULL COMMENT 'Client IP address',
  username VARCHAR(64) DEFAULT NULL COMMENT 'Operating username',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_error_log_trace_id (trace_id),
  KEY idx_error_log_create_time (create_time),
  KEY idx_error_log_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Error log';

-- ============================================================
-- sys_operation_log table
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
  request_method VARCHAR(10) DEFAULT NULL COMMENT 'Request method (GET/POST/PUT/DELETE)',
  request_url VARCHAR(255) DEFAULT NULL COMMENT 'Request URL',
  client_ip VARCHAR(64) DEFAULT NULL COMMENT 'Client IP address',
  username VARCHAR(64) DEFAULT NULL COMMENT 'Operating username',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  PRIMARY KEY (id),
  KEY idx_operation_log_trace_id (trace_id),
  KEY idx_operation_log_create_time (create_time),
  KEY idx_operation_log_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Operation log';

-- ============================================================
-- sys_file_upload table
-- Records metadata of uploaded files
-- ============================================================
DROP TABLE IF EXISTS sys_file_upload;

CREATE TABLE IF NOT EXISTS sys_file_upload (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  file_type INT DEFAULT NULL COMMENT 'File type (1=image, 2=document, 3=archive, 9=other)',
  stored_name VARCHAR(128) DEFAULT NULL COMMENT 'UUID-based storage filename',
  original_filename VARCHAR(255) DEFAULT NULL COMMENT 'Original filename from client',
  extension VARCHAR(32) DEFAULT NULL COMMENT 'File extension (lowercase, without dot)',
  file_size BIGINT DEFAULT NULL COMMENT 'File size in bytes',
  content_type VARCHAR(128) DEFAULT NULL COMMENT 'MIME type',
  file_path VARCHAR(512) DEFAULT NULL COMMENT 'Full relative path from upload directory',
  date_path VARCHAR(32) DEFAULT NULL COMMENT 'Date-based subdirectory (e.g., "2026/06/03")',
  date_url VARCHAR(32) DEFAULT NULL COMMENT 'Date in URL-friendly format (e.g., "2026-06-03")',
  associate_type VARCHAR(64) DEFAULT NULL COMMENT 'Association type (e.g., USER_AVATAR, TICKET_ATTACHMENT)',
  associate_id BIGINT DEFAULT NULL COMMENT 'Associated business entity ID',
  md5 VARCHAR(32) DEFAULT NULL COMMENT 'MD5 hash of file content',
  creator VARCHAR(64) DEFAULT NULL COMMENT 'Record creation user',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  deleted TINYINT(1) DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (id),
  KEY idx_file_upload_associate (associate_type, associate_id),
  KEY idx_file_upload_create_time (create_time),
  KEY idx_file_upload_creator (creator)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'File upload';
