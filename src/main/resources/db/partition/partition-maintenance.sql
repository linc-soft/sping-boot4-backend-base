-- ============================================================
-- Partition Maintenance Stored Procedures (REQUIRED when using partitions)
--
-- Why this file is needed:
--   The initial DDL (partition-monthly.sql / partition-yearly.sql)
--   only creates a limited number of partitions + one p_future catch-all.
--   Over time, if new partitions are not created, all new data accumulates
--   in p_future, making partitioning pointless.
--   The stored procedures in this file automatically split new partitions
--   from p_future, and optionally clean up / archive expired partitions.
--
-- Deployment steps:
--   1. Execute this file to create 4 stored procedures
--   2. Enable MySQL Event Scheduler:
--        SET GLOBAL event_scheduler = ON;
--        or add to my.cnf: event_scheduler = ON
--   3. Uncomment the corresponding Event at the bottom of this file,
--      replace 'your_schema' with your actual database name:
--      - Monthly partition -> enable evt_monthly_partition_maintenance
--      - Yearly partition  -> enable evt_yearly_partition_maintenance
--      - Expiry cleanup    -> enable evt_drop_expired_partitions
--                             or evt_archive_expired_partitions
--
-- Stored procedures included:
--   sp_create_monthly_partitions  - Auto-create future N monthly partitions
--   sp_create_yearly_partitions   - Auto-create future N yearly partitions
--   sp_drop_expired_partitions    - Drop expired partitions directly
--   sp_archive_expired_partitions - Archive to backup table then drop
-- ============================================================

DELIMITER $$

-- ============================================================
-- Stored procedure: Auto-create future N monthly partitions
-- Parameters:
--   p_table_name   - Table name
--   p_schema_name  - Database/schema name
--   p_months_ahead - Number of months ahead to create (recommended: 3-6)
-- Logic:
--   Iterates from current month, checks if partition exists,
--   and uses REORGANIZE PARTITION p_future to split out new partitions
-- ============================================================
DROP PROCEDURE IF EXISTS sp_create_monthly_partitions$$

CREATE PROCEDURE sp_create_monthly_partitions(
  IN p_table_name  VARCHAR(64),
  IN p_schema_name VARCHAR(64),
  IN p_months_ahead INT
)
BEGIN
  DECLARE v_month_start DATE;
  DECLARE v_month_end DATE;
  DECLARE v_partition_name VARCHAR(16);
  DECLARE v_partition_exists INT DEFAULT 0;
  DECLARE v_counter INT DEFAULT 0;
  DECLARE v_sql TEXT;

  WHILE v_counter <= p_months_ahead DO
    SET v_month_start = DATE_FORMAT(DATE_ADD(CURRENT_DATE, INTERVAL v_counter MONTH), '%Y-%m-01');
    SET v_month_end   = DATE_FORMAT(DATE_ADD(v_month_start, INTERVAL 1 MONTH), '%Y-%m-01');
    SET v_partition_name = CONCAT('p', DATE_FORMAT(v_month_start, '%Y%m'));

    -- Check if partition already exists
    SELECT COUNT(*) INTO v_partition_exists
    FROM INFORMATION_SCHEMA.PARTITIONS
    WHERE TABLE_SCHEMA = p_schema_name
      AND TABLE_NAME = p_table_name
      AND PARTITION_NAME = v_partition_name;

    IF v_partition_exists = 0 THEN
      SET v_sql = CONCAT(
        'ALTER TABLE `', p_schema_name, '`.`', p_table_name, '` ',
        'REORGANIZE PARTITION p_future INTO (',
        'PARTITION ', v_partition_name, ' VALUES LESS THAN (TO_DAYS(''', v_month_end, ''')), ',
        'PARTITION p_future VALUES LESS THAN MAXVALUE',
        ')'
      );

      SET @v_dynamic_sql = v_sql;
      PREPARE stmt FROM @v_dynamic_sql;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;
    END IF;

    SET v_counter = v_counter + 1;
  END WHILE;
END$$

-- ============================================================
-- Stored procedure: Auto-create future N yearly partitions
-- Parameters:
--   p_table_name  - Table name
--   p_schema_name - Database/schema name
--   p_years_ahead - Number of years ahead to create (recommended: 2-3)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_create_yearly_partitions$$

CREATE PROCEDURE sp_create_yearly_partitions(
  IN p_table_name  VARCHAR(64),
  IN p_schema_name VARCHAR(64),
  IN p_years_ahead INT
)
BEGIN
  DECLARE v_year INT;
  DECLARE v_partition_name VARCHAR(16);
  DECLARE v_partition_exists INT DEFAULT 0;
  DECLARE v_counter INT DEFAULT 0;
  DECLARE v_sql TEXT;

  SET v_year = YEAR(CURRENT_DATE);

  WHILE v_counter <= p_years_ahead DO
    SET v_partition_name = CONCAT('p', v_year + v_counter);

    SELECT COUNT(*) INTO v_partition_exists
    FROM INFORMATION_SCHEMA.PARTITIONS
    WHERE TABLE_SCHEMA = p_schema_name
      AND TABLE_NAME = p_table_name
      AND PARTITION_NAME = v_partition_name;

    IF v_partition_exists = 0 THEN
      SET v_sql = CONCAT(
        'ALTER TABLE `', p_schema_name, '`.`', p_table_name, '` ',
        'REORGANIZE PARTITION p_future INTO (',
        'PARTITION ', v_partition_name, ' VALUES LESS THAN (', v_year + v_counter + 1, '), ',
        'PARTITION p_future VALUES LESS THAN MAXVALUE',
        ')'
      );

      SET @v_dynamic_sql = v_sql;
      PREPARE stmt FROM @v_dynamic_sql;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;
    END IF;

    SET v_counter = v_counter + 1;
  END WHILE;
END$$

-- ============================================================
-- Stored procedure: Drop expired partitions directly
-- Parameters:
--   p_table_name     - Table name
--   p_schema_name    - Database/schema name
--   p_retention_days - Data retention period in days
-- WARNING:
--   DROP PARTITION is a DDL operation and cannot be rolled back.
--   Export data first if you need to preserve it.
-- ============================================================
DROP PROCEDURE IF EXISTS sp_drop_expired_partitions$$

CREATE PROCEDURE sp_drop_expired_partitions(
  IN p_table_name     VARCHAR(64),
  IN p_schema_name    VARCHAR(64),
  IN p_retention_days INT
)
BEGIN
  DECLARE v_partition_name VARCHAR(64);
  DECLARE v_partition_desc VARCHAR(64);
  DECLARE v_done INT DEFAULT 0;
  DECLARE v_cutoff_days INT;
  DECLARE v_sql TEXT;

  SET v_cutoff_days = TO_DAYS(DATE_SUB(CURRENT_DATE, INTERVAL p_retention_days DAY));

  DECLARE cur CURSOR FOR
    SELECT PARTITION_NAME, PARTITION_DESCRIPTION
    FROM INFORMATION_SCHEMA.PARTITIONS
    WHERE TABLE_SCHEMA = p_schema_name
      AND TABLE_NAME = p_table_name
      AND PARTITION_NAME != 'p_future'
      AND PARTITION_NAME != 'p_init'
      AND PARTITION_DESCRIPTION != 'MAXVALUE'
    ORDER BY PARTITION_ORDINAL_POSITION;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

  OPEN cur;

  read_loop: LOOP
    FETCH cur INTO v_partition_name, v_partition_desc;
    IF v_done THEN
      LEAVE read_loop;
    END IF;

    IF CAST(v_partition_desc AS UNSIGNED) <= v_cutoff_days THEN
      SET v_sql = CONCAT(
        'ALTER TABLE `', p_schema_name, '`.`', p_table_name, '` ',
        'DROP PARTITION ', v_partition_name
      );

      SET @v_dynamic_sql = v_sql;
      PREPARE stmt FROM @v_dynamic_sql;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;
    END IF;
  END LOOP;

  CLOSE cur;
END$$

-- ============================================================
-- Stored procedure: Archive expired partitions to backup table then drop
-- Parameters:
--   p_table_name     - Source table name
--   p_schema_name    - Database/schema name
--   p_retention_days - Data retention period in days
-- Notes:
--   Archive table naming convention: {original_table}_archive
--   Archive table must be created in advance (same structure, no partitions)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_archive_expired_partitions$$

CREATE PROCEDURE sp_archive_expired_partitions(
  IN p_table_name     VARCHAR(64),
  IN p_schema_name    VARCHAR(64),
  IN p_retention_days INT
)
BEGIN
  DECLARE v_partition_name VARCHAR(64);
  DECLARE v_partition_desc VARCHAR(64);
  DECLARE v_done INT DEFAULT 0;
  DECLARE v_cutoff_days INT;
  DECLARE v_archive_table VARCHAR(128);
  DECLARE v_sql TEXT;

  SET v_cutoff_days = TO_DAYS(DATE_SUB(CURRENT_DATE, INTERVAL p_retention_days DAY));
  SET v_archive_table = CONCAT(p_table_name, '_archive');

  DECLARE cur CURSOR FOR
    SELECT PARTITION_NAME, PARTITION_DESCRIPTION
    FROM INFORMATION_SCHEMA.PARTITIONS
    WHERE TABLE_SCHEMA = p_schema_name
      AND TABLE_NAME = p_table_name
      AND PARTITION_NAME != 'p_future'
      AND PARTITION_NAME != 'p_init'
      AND PARTITION_DESCRIPTION != 'MAXVALUE'
    ORDER BY PARTITION_ORDINAL_POSITION;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

  OPEN cur;

  read_loop: LOOP
    FETCH cur INTO v_partition_name, v_partition_desc;
    IF v_done THEN
      LEAVE read_loop;
    END IF;

    IF CAST(v_partition_desc AS UNSIGNED) <= v_cutoff_days THEN
      -- Copy data to archive table
      SET v_sql = CONCAT(
        'INSERT INTO `', p_schema_name, '`.`', v_archive_table, '` ',
        'SELECT * FROM `', p_schema_name, '`.`', p_table_name, '` ',
        'PARTITION (', v_partition_name, ')'
      );

      SET @v_dynamic_sql = v_sql;
      PREPARE stmt FROM @v_dynamic_sql;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;

      -- Drop the partition
      SET v_sql = CONCAT(
        'ALTER TABLE `', p_schema_name, '`.`', p_table_name, '` ',
        'DROP PARTITION ', v_partition_name
      );

      SET @v_dynamic_sql = v_sql;
      PREPARE stmt FROM @v_dynamic_sql;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;
    END IF;
  END LOOP;

  CLOSE cur;
END$$

DELIMITER ;

-- ============================================================
-- MySQL Event Scheduler examples
-- NOTE: Event Scheduler must be enabled first:
--   SET GLOBAL event_scheduler = ON;
--   or add to my.cnf: event_scheduler = ON
-- Replace 'your_schema' with your actual database name
-- ============================================================

-- Example 1: Monthly partition - daily at 02:00, create next 3 months
/*
CREATE EVENT IF NOT EXISTS evt_monthly_partition_maintenance
  ON SCHEDULE EVERY 1 DAY
  STARTS CURRENT_DATE + INTERVAL 2 HOUR
  DO
  BEGIN
    CALL sp_create_monthly_partitions('sys_access_log', 'your_schema', 3);
    CALL sp_create_monthly_partitions('sys_error_log', 'your_schema', 3);
    CALL sp_create_monthly_partitions('sys_operation_log', 'your_schema', 3);
  END;
*/

-- Example 2: Yearly partition - monthly on 1st at 02:00, create next 2 years
/*
CREATE EVENT IF NOT EXISTS evt_yearly_partition_maintenance
  ON SCHEDULE EVERY 1 MONTH
  STARTS DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') + INTERVAL 1 MONTH + INTERVAL 2 HOUR
  DO
  BEGIN
    CALL sp_create_yearly_partitions('sys_access_log', 'your_schema', 2);
    CALL sp_create_yearly_partitions('sys_error_log', 'your_schema', 2);
    CALL sp_create_yearly_partitions('sys_operation_log', 'your_schema', 2);
  END;
*/

-- Example 3: Monthly on 1st at 03:00, drop partitions older than 180 days
/*
CREATE EVENT IF NOT EXISTS evt_drop_expired_partitions
  ON SCHEDULE EVERY 1 MONTH
  STARTS DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') + INTERVAL 1 MONTH + INTERVAL 3 HOUR
  DO
  BEGIN
    CALL sp_drop_expired_partitions('sys_access_log', 'your_schema', 180);
    CALL sp_drop_expired_partitions('sys_error_log', 'your_schema', 180);
    CALL sp_drop_expired_partitions('sys_operation_log', 'your_schema', 180);
  END;
*/

-- Example 4: Monthly on 1st at 03:00, archive then drop partitions older than 180 days
/*
CREATE EVENT IF NOT EXISTS evt_archive_expired_partitions
  ON SCHEDULE EVERY 1 MONTH
  STARTS DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') + INTERVAL 1 MONTH + INTERVAL 3 HOUR
  DO
  BEGIN
    CALL sp_archive_expired_partitions('sys_access_log', 'your_schema', 180);
    CALL sp_archive_expired_partitions('sys_error_log', 'your_schema', 180);
    CALL sp_archive_expired_partitions('sys_operation_log', 'your_schema', 180);
  END;
*/