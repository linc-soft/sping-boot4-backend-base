-- ============================================================
-- Online Migration Script: Convert non-partitioned tables to partitioned tables
-- No downtime required. Existing data is automatically distributed to partitions.
--
-- Prerequisites:
--   1. MySQL 8.0+ (Online DDL support)
--   2. create_time column must not contain NULL values
--   3. Recommended to execute during off-peak hours (DDL rebuilds the table)
--
-- Execution order: Step 1 -> Step 2 (choose either 2A or 2B)
-- ============================================================
-- ============================================================
-- Pre-check: Verify create_time has no NULL values
-- If any result returns non-zero, fix the data before proceeding
-- ============================================================
SELECT
    'sys_access_log' AS table_name,
    COUNT(*) AS null_count
FROM
    sys_access_log
WHERE
    create_time IS NULL
UNION ALL
SELECT
    'sys_error_log',
    COUNT(*)
FROM
    sys_error_log
WHERE
    create_time IS NULL
UNION ALL
SELECT
    'sys_operation_log',
    COUNT(*)
FROM
    sys_operation_log
WHERE
    create_time IS NULL;

-- If NULL values exist, fix them first (adjust the date as needed):
-- UPDATE sys_access_log    SET create_time = '2026-04-01 00:00:00' WHERE create_time IS NULL;
-- UPDATE sys_error_log     SET create_time = '2026-04-01 00:00:00' WHERE create_time IS NULL;
-- UPDATE sys_operation_log SET create_time = '2026-04-01 00:00:00' WHERE create_time IS NULL;
-- ============================================================
-- Step 1: Modify primary key to include create_time
-- InnoDB Online DDL rebuilds the table without blocking DML.
-- For large tables, consider using pt-online-schema-change.
-- ============================================================
ALTER TABLE sys_access_log
DROP PRIMARY KEY,
ADD PRIMARY KEY (id, create_time);

ALTER TABLE sys_error_log
DROP PRIMARY KEY,
ADD PRIMARY KEY (id, create_time);

ALTER TABLE sys_operation_log
DROP PRIMARY KEY,
ADD PRIMARY KEY (id, create_time);

-- ============================================================
-- Step 2A: Add monthly partitions (choose either 2A or 2B)
-- Existing data is automatically distributed to matching partitions.
-- Adjust the initial partition list based on your existing data range.
-- ============================================================
/*
ALTER TABLE sys_access_log
PARTITION BY RANGE (TO_DAYS(create_time)) (
PARTITION p_init   VALUES LESS THAN (TO_DAYS('2026-05-01')) COMMENT 'Initial partition (includes historical data)',
PARTITION p_future VALUES LESS THAN MAXVALUE                COMMENT 'Catch-all partition'
);

ALTER TABLE sys_error_log
PARTITION BY RANGE (TO_DAYS(create_time)) (
PARTITION p_init   VALUES LESS THAN (TO_DAYS('2026-05-01')) COMMENT 'Initial partition (includes historical data)',
PARTITION p_future VALUES LESS THAN MAXVALUE                COMMENT 'Catch-all partition'
);

ALTER TABLE sys_operation_log
PARTITION BY RANGE (TO_DAYS(create_time)) (
PARTITION p_init   VALUES LESS THAN (TO_DAYS('2026-05-01')) COMMENT 'Initial partition (includes historical data)',
PARTITION p_future VALUES LESS THAN MAXVALUE                COMMENT 'Catch-all partition'
);
*/
-- ============================================================
-- Step 2B: Add yearly partitions (choose either 2A or 2B)
-- ============================================================
/*
ALTER TABLE sys_access_log
PARTITION BY RANGE (YEAR(create_time)) (
PARTITION p_init   VALUES LESS THAN (2027) COMMENT 'Initial partition (includes historical data)',
PARTITION p_future VALUES LESS THAN MAXVALUE COMMENT 'Catch-all partition'
);

ALTER TABLE sys_error_log
PARTITION BY RANGE (YEAR(create_time)) (
PARTITION p_init   VALUES LESS THAN (2027) COMMENT 'Initial partition (includes historical data)',
PARTITION p_future VALUES LESS THAN MAXVALUE COMMENT 'Catch-all partition'
);

ALTER TABLE sys_operation_log
PARTITION BY RANGE (YEAR(create_time)) (
PARTITION p_init   VALUES LESS THAN (2027) COMMENT 'Initial partition (includes historical data)',
PARTITION p_future VALUES LESS THAN MAXVALUE COMMENT 'Catch-all partition'
);
*/
-- ============================================================
-- Verification: Check partition information
-- ============================================================
SELECT
    TABLE_NAME,
    PARTITION_NAME,
    PARTITION_ORDINAL_POSITION,
    PARTITION_DESCRIPTION,
    TABLE_ROWS
FROM
    INFORMATION_SCHEMA.PARTITIONS
WHERE
    TABLE_SCHEMA = DATABASE ()
    AND TABLE_NAME IN (
        'sys_access_log',
        'sys_error_log',
        'sys_operation_log'
    )
ORDER BY
    TABLE_NAME,
    PARTITION_ORDINAL_POSITION;