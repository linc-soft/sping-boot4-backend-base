# Spring Boot 4 Backend Base

[English](#english) | [中文](#中文) | [日本語](#日本語)

<a name="english"></a>

## English

This is an enterprise-grade backend framework built on **Spring Boot 4 + Java 25**, designed to provide user authentication, authorization, and data management for frontend applications.

### Architecture & Technology Stack

- **Architecture**: Frontend-backend separated architecture
- **Authentication**: Stateless authentication using Spring Security + JWT
- **ORM**: MyBatisPlus
- **Database**: MySQL
- **Cache & Token Management**: Redis

### Core Design Goals

- **Unified Response Format**: Consistent API response structure
- **Comprehensive Logging**: Access logs, exception logs, and operation logs correlated via stack trace ID
- **Asynchronous Logging**: Ensures system performance through async log writing
- **Security Protection**: CSRF protection, CORS configuration, and password encryption
- **Multi-Environment Support**: Development, Testing, and Production environments

### Deployment: Reverse Proxy Configuration (Important)

This project uses `X-Forwarded-For` and other proxy headers to identify the real client IP address. This affects both **rate limiting** (`RateLimitFilter`) and **access logging** (`AccessLogInterceptor`).

If the reverse proxy does not properly overwrite these headers, clients can forge `X-Forwarded-For` to bypass rate limiting or pollute access logs.

#### Nginx Configuration Example

```nginx
server {
    listen 80;
    server_name example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;

        # Overwrite X-Forwarded-For with the actual TCP peer address
        # This prevents clients from injecting fake IPs
        proxy_set_header X-Forwarded-For $remote_addr;

        # Pass the real client IP
        proxy_set_header X-Real-IP $remote_addr;

        # Pass the original Host header
        proxy_set_header Host $host;

        # Disable proxy buffering for streaming responses (optional)
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> **Key point**: Use `$remote_addr` (not `$proxy_add_x_forwarded_for`) to overwrite `X-Forwarded-For`. Using `$proxy_add_x_forwarded_for` appends to the existing header, which means a client-supplied fake value is preserved.

> For multi-layer proxy setups (CDN → Load Balancer → Nginx → App), ensure only the outermost proxy sets `X-Forwarded-For`, and inner proxies pass it through without appending.

### Deployment: Monitoring with Prometheus + Grafana

This project integrates Spring Boot Actuator with Micrometer Prometheus Registry. The management endpoints run on an independent port (`9090`) separated from the business port (`8080`).

#### Environment-specific Endpoint Exposure

| Environment | Exposed Endpoints                                                    |
| ----------- | -------------------------------------------------------------------- |
| dev         | health, info, prometheus, metrics, beans, env, configprops, mappings |
| test        | health, info, prometheus, metrics                                    |
| prod        | health, info, prometheus                                             |

#### Security: Network Isolation

The management port `9090` must NOT be exposed to the public internet. Only allow access from the Prometheus server IP via firewall rules.

```bash
# iptables example: allow only Prometheus server (192.168.1.100) to access port 9090
iptables -A INPUT -p tcp --dport 9090 -s 192.168.1.100 -j ACCEPT
iptables -A INPUT -p tcp --dport 9090 -j DROP
```

#### Docker Compose (Prometheus + Alertmanager + Grafana)

```yaml
version: "3.8"
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9091:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/alert-rules.yml:/etc/prometheus/alert-rules.yml
      - ./monitoring/targets:/etc/prometheus/targets
      - prometheus_data:/prometheus
    command:
      - "--config.file=/etc/prometheus/prometheus.yml"
      - "--storage.tsdb.retention.time=15d"
    restart: unless-stopped

  alertmanager:
    image: prom/alertmanager:latest
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD:-admin}
    volumes:
      - grafana_data:/var/lib/grafana
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
```

#### Prometheus Configuration (`monitoring/prometheus.yml`)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert-rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: ["alertmanager:9093"]

scrape_configs:
  - job_name: "spring-boot-app"
    metrics_path: "/actuator/prometheus"
    file_sd_configs:
      - files:
          - "/etc/prometheus/targets/*.json"
        refresh_interval: 30s
```

#### Service Discovery Targets (`monitoring/targets/app.json`)

```json
[
  {
    "targets": ["192.168.1.1:9090", "192.168.1.2:9090", "192.168.1.3:9090"],
    "labels": { "env": "production", "app": "backend-base" }
  }
]
```

Update the IP addresses and ports to match your actual deployment.

#### Alert Rules (`monitoring/alert-rules.yml`)

```yaml
groups:
  - name: spring-boot-alerts
    rules:
      - alert: InstanceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Instance {{ $labels.instance }} is down"

      - alert: HighResponseTime
        expr: http_server_requests_seconds_max{uri!~"/actuator.*"} > 5
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Endpoint {{ $labels.uri }} response time exceeds 5s"

      - alert: HighHeapUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Instance {{ $labels.instance }} heap usage exceeds 90%"
```

#### Alertmanager Email Configuration (`monitoring/alertmanager.yml`)

```yaml
global:
  smtp_smarthost: "smtp.example.com:587"
  smtp_from: "alertmanager@example.com"
  smtp_auth_username: "alertmanager@example.com"
  smtp_auth_password: "your-smtp-password"
  smtp_require_tls: true

route:
  receiver: "email-notifications"
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: "email-notifications"
    email_configs:
      - to: "ops-team@example.com"
        send_resolved: true
```

Replace SMTP settings with your actual mail server configuration.

#### Grafana Dashboard Setup

After Grafana starts (default: `http://localhost:3000`, admin/admin):

1. Add Prometheus data source: URL = `http://prometheus:9090`
2. Import community dashboards by ID:
   - `4701` — JVM Micrometer Dashboard
   - `12900` — Spring Boot Statistics
   - `1860` — Node Exporter (system-level metrics)

Additional dashboards can be imported at any time from the Grafana UI without restart.

### Deployment: Log Table Partition Strategy

This project provides optional MySQL RANGE partition support for the three system log tables:
`sys_access_log`, `sys_error_log`, `sys_operation_log`.

Partitioning improves query performance on time-range queries via **partition pruning**
(MySQL automatically skips irrelevant partitions) and enables efficient data lifecycle
management (drop an entire partition instead of row-level `DELETE`).

#### File Reference

All partition-related SQL files are located in `src/main/resources/db/partition/`:

| File                        | Description                                                                                          |
| --------------------------- | ---------------------------------------------------------------------------------------------------- |
| `partition-monthly.sql`     | Monthly partition DDL — create tables with monthly partitions                                        |
| `partition-yearly.sql`      | Yearly partition DDL — create tables with yearly partitions                                          |
| `partition-maintenance.sql` | Stored procedures + Event Scheduler for auto-creating future partitions and cleaning up expired ones |
| `alter-to-partition.sql`    | Online migration script — convert existing non-partitioned tables to partitioned tables              |

> The non-partitioned DDL is located at `src/main/resources/db/system.sql`.

#### Choosing a Strategy

| Strategy                          | When to Use                                                   | Partition Key          |
| --------------------------------- | ------------------------------------------------------------- | ---------------------- |
| No partition (`system.sql`)       | Low traffic, simple deployment                                | N/A                    |
| Monthly (`partition-monthly.sql`) | High volume (100k+ rows/day), need monthly archival           | `TO_DAYS(create_time)` |
| Yearly (`partition-yearly.sql`)   | Medium volume (<100k rows/day), yearly archival is sufficient | `YEAR(create_time)`    |

#### Scenario A: Fresh Deployment (No Partition)

Use the standard DDL. No additional steps required.

```sql
SOURCE src/main/resources/db/system.sql;
```

#### Scenario B: Fresh Deployment (With Partition)

**Step 1** — Create partitioned tables (choose one):

```sql
-- Monthly partition
SOURCE src/main/resources/db/partition/partition-monthly.sql;

-- OR Yearly partition
SOURCE src/main/resources/db/partition/partition-yearly.sql;
```

**Step 2** — Deploy maintenance stored procedures:

```sql
SOURCE src/main/resources/db/partition/partition-maintenance.sql;
```

**Step 3** — Generate future partitions (replace `your_schema` with actual database name):

```sql
-- Monthly: create next 6 months
CALL sp_create_monthly_partitions('sys_access_log', 'your_schema', 6);
CALL sp_create_monthly_partitions('sys_error_log', 'your_schema', 6);
CALL sp_create_monthly_partitions('sys_operation_log', 'your_schema', 6);

-- OR Yearly: create next 3 years
CALL sp_create_yearly_partitions('sys_access_log', 'your_schema', 3);
CALL sp_create_yearly_partitions('sys_error_log', 'your_schema', 3);
CALL sp_create_yearly_partitions('sys_operation_log', 'your_schema', 3);
```

**Step 4** — Enable Event Scheduler for automatic partition creation:

```sql
SET GLOBAL event_scheduler = ON;
```

Then uncomment the corresponding Event definition at the bottom of `partition-maintenance.sql` and replace `your_schema` with your database name.

#### Scenario C: Migrate Running System (No Partition → Partition)

For systems already running with non-partitioned tables, use `alter-to-partition.sql` to convert in-place. **No downtime required.** Existing data is automatically distributed to the correct partitions.

**Step 1** — Verify `create_time` has no NULL values:

```sql
SELECT 'sys_access_log' AS table_name, COUNT(*) AS null_count
FROM sys_access_log WHERE create_time IS NULL
UNION ALL
SELECT 'sys_error_log', COUNT(*) FROM sys_error_log WHERE create_time IS NULL
UNION ALL
SELECT 'sys_operation_log', COUNT(*) FROM sys_operation_log WHERE create_time IS NULL;
```

**Step 2** — Modify primary key (Online DDL, no downtime):

```sql
ALTER TABLE sys_access_log    DROP PRIMARY KEY, ADD PRIMARY KEY (id, create_time);
ALTER TABLE sys_error_log     DROP PRIMARY KEY, ADD PRIMARY KEY (id, create_time);
ALTER TABLE sys_operation_log DROP PRIMARY KEY, ADD PRIMARY KEY (id, create_time);
```

> For very large tables, consider using `pt-online-schema-change`.

**Step 3** — Add partitions (choose monthly or yearly, see `alter-to-partition.sql`).

**Step 4** — Deploy maintenance and generate future partitions (same as Scenario B, Steps 2-4).

#### Partition Maintenance

The stored procedures `sp_create_monthly_partitions` / `sp_create_yearly_partitions` use `REORGANIZE PARTITION p_future` to split new partitions from the catch-all partition. Recommended Event Scheduler setup:

- Monthly partitions: run daily, create 3 months ahead
- Yearly partitions: run monthly, create 2 years ahead

#### Expired Partition Cleanup (Optional)

| Procedure                       | Behavior                                                                                       |
| ------------------------------- | ---------------------------------------------------------------------------------------------- |
| `sp_drop_expired_partitions`    | Directly drops partitions older than N days. Fast, frees disk immediately. **Irreversible.**   |
| `sp_archive_expired_partitions` | Copies data to `{table}_archive` table first, then drops. Requires pre-created archive tables. |

> Not cleaning up expired partitions does **not** affect query performance for time-range queries (partition pruning still works). The main impact is disk space consumption.

---

<a name="中文"></a>

## 中文

本项目是一个基于 **Spring Boot 4 + Java 25** 的企业级后端框架，为前端应用提供用户认证、授权和数据管理功能。

### 架构与技术栈

- **架构设计**: 前后端分离架构
- **认证机制**: 基于 Spring Security + JWT 的无状态认证
- **ORM 框架**: MyBatisPlus
- **数据库**: MySQL
- **缓存与 Token 管理**: Redis

### 核心设计目标

- **统一响应格式**: 规范化的 API 响应结构
- **完善的日志体系**: 访问日志、异常日志、操作日志通过堆栈 ID 进行关联
- **异步日志写入**: 通过异步日志保证系统高性能
- **安全防护**: CSRF 保护、CORS 配置、密码加密
- **多环境支持**: 支持开发、测试、生产等多环境配置

### 部署须知：反向代理配置（重要）

本项目通过 `X-Forwarded-For` 等代理头获取真实客户端 IP，影响范围包括**限流**（`RateLimitFilter`）和**访问日志**（`AccessLogInterceptor`）。

如果反向代理未正确覆写这些头部，客户端可以伪造 `X-Forwarded-For` 来绕过限流或污染访问日志。

#### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;

        # 使用实际 TCP 对端地址覆写 X-Forwarded-For，防止客户端注入伪造 IP
        proxy_set_header X-Forwarded-For $remote_addr;

        # 传递真实客户端 IP
        proxy_set_header X-Real-IP $remote_addr;

        # 传递原始 Host 头
        proxy_set_header Host $host;

        # 传递协议类型
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> **关键点**：使用 `$remote_addr`（而非 `$proxy_add_x_forwarded_for`）来覆写 `X-Forwarded-For`。`$proxy_add_x_forwarded_for` 会追加到已有头部，客户端伪造的值会被保留。

> 多层代理场景（CDN → 负载均衡 → Nginx → 应用）下，确保只有最外层代理设置 `X-Forwarded-For`，内层代理透传即可。

### 部署须知：Prometheus + Grafana 监控

本项目集成了 Spring Boot Actuator 和 Micrometer Prometheus Registry。管理端点运行在独立端口（`9090`），与业务端口（`8080`）隔离。

#### 各环境端点暴露策略

| 环境 | 暴露的端点                                                           |
| ---- | -------------------------------------------------------------------- |
| dev  | health, info, prometheus, metrics, beans, env, configprops, mappings |
| test | health, info, prometheus, metrics                                    |
| prod | health, info, prometheus                                             |

#### 安全：网络层隔离

管理端口 `9090` 禁止对公网暴露，仅允许 Prometheus 服务器 IP 通过防火墙访问。

```bash
# iptables 示例：仅允许 Prometheus 服务器（192.168.1.100）访问 9090 端口
iptables -A INPUT -p tcp --dport 9090 -s 192.168.1.100 -j ACCEPT
iptables -A INPUT -p tcp --dport 9090 -j DROP
```

#### Docker Compose 部署（Prometheus + Alertmanager + Grafana）

```yaml
version: "3.8"
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9091:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/alert-rules.yml:/etc/prometheus/alert-rules.yml
      - ./monitoring/targets:/etc/prometheus/targets
      - prometheus_data:/prometheus
    command:
      - "--config.file=/etc/prometheus/prometheus.yml"
      - "--storage.tsdb.retention.time=15d"
    restart: unless-stopped

  alertmanager:
    image: prom/alertmanager:latest
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD:-admin}
    volumes:
      - grafana_data:/var/lib/grafana
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
```

#### Prometheus 配置（`monitoring/prometheus.yml`）

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert-rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: ["alertmanager:9093"]

scrape_configs:
  - job_name: "spring-boot-app"
    metrics_path: "/actuator/prometheus"
    file_sd_configs:
      - files:
          - "/etc/prometheus/targets/*.json"
        refresh_interval: 30s
```

#### 服务发现目标文件（`monitoring/targets/app.json`）

```json
[
  {
    "targets": ["192.168.1.1:9090", "192.168.1.2:9090", "192.168.1.3:9090"],
    "labels": { "env": "production", "app": "backend-base" }
  }
]
```

请根据实际部署环境修改 IP 地址和端口。

#### 告警规则（`monitoring/alert-rules.yml`）

```yaml
groups:
  - name: spring-boot-alerts
    rules:
      - alert: InstanceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "实例 {{ $labels.instance }} 已宕机"

      - alert: HighResponseTime
        expr: http_server_requests_seconds_max{uri!~"/actuator.*"} > 5
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "接口 {{ $labels.uri }} 响应时间超过 5 秒"

      - alert: HighHeapUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "实例 {{ $labels.instance }} 堆内存使用率超过 90%"
```

#### Alertmanager 邮件告警配置（`monitoring/alertmanager.yml`）

```yaml
global:
  smtp_smarthost: "smtp.example.com:587"
  smtp_from: "alertmanager@example.com"
  smtp_auth_username: "alertmanager@example.com"
  smtp_auth_password: "your-smtp-password"
  smtp_require_tls: true

route:
  receiver: "email-notifications"
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: "email-notifications"
    email_configs:
      - to: "ops-team@example.com"
        send_resolved: true
```

请将 SMTP 配置替换为实际的邮件服务器信息。

#### Grafana 仪表盘配置

Grafana 启动后（默认地址：`http://localhost:3000`，初始账号 admin/admin）：

1. 添加 Prometheus 数据源：URL 填 `http://prometheus:9090`
2. 通过 ID 导入社区仪表盘：
   - `4701` — JVM Micrometer 监控面板
   - `12900` — Spring Boot 统计面板
   - `1860` — Node Exporter 系统级指标面板

后续可随时在 Grafana UI 中导入新的仪表盘，无需重启。

### 部署须知：日志表分区策略

本项目为三张系统日志表（`sys_access_log`、`sys_error_log`、`sys_operation_log`）提供了可选的 MySQL RANGE 分区支持。

分区通过 **partition pruning**（MySQL 自动跳过无关分区）提升时间范围查询性能，并支持高效的数据生命周期管理（直接 DROP 整个分区，而非逐行 DELETE）。

#### 文件说明

所有分区相关 SQL 文件位于 `src/main/resources/db/partition/`：

| 文件                        | 说明                                                                |
| --------------------------- | ------------------------------------------------------------------- |
| `partition-monthly.sql`     | 月度分区 DDL — 按月创建分区表                                       |
| `partition-yearly.sql`      | 年度分区 DDL — 按年创建分区表                                       |
| `partition-maintenance.sql` | 存储过程 + Event Scheduler 定时任务，自动创建未来分区和清理过期分区 |
| `alter-to-partition.sql`    | 在线迁移脚本 — 将现有非分区表原地转为分区表                         |

> 非分区 DDL 位于 `src/main/resources/db/system.sql`。

#### 策略选择

| 策略                                | 适用场景                                  | 分区键                 |
| ----------------------------------- | ----------------------------------------- | ---------------------- |
| 不分区（`system.sql`）              | 流量低，部署简单                          | 无                     |
| 月度分区（`partition-monthly.sql`） | 日志量大（日均 10w+ 条），需按月归档      | `TO_DAYS(create_time)` |
| 年度分区（`partition-yearly.sql`）  | 日志量中等（日均 < 10w 条），按年归档即可 | `YEAR(create_time)`    |

#### 场景 A：全新部署（不使用分区）

使用标准 DDL，无需额外步骤。

```sql
SOURCE src/main/resources/db/system.sql;
```

#### 场景 B：全新部署（使用分区）

**步骤 1** — 创建分区表（二选一）：

```sql
-- 月度分区
SOURCE src/main/resources/db/partition/partition-monthly.sql;

-- 或 年度分区
SOURCE src/main/resources/db/partition/partition-yearly.sql;
```

**步骤 2** — 部署维护存储过程：

```sql
SOURCE src/main/resources/db/partition/partition-maintenance.sql;
```

**步骤 3** — 生成未来分区（将 `your_schema` 替换为实际数据库名）：

```sql
-- 月度分区：创建未来 6 个月
CALL sp_create_monthly_partitions('sys_access_log', 'your_schema', 6);
CALL sp_create_monthly_partitions('sys_error_log', 'your_schema', 6);
CALL sp_create_monthly_partitions('sys_operation_log', 'your_schema', 6);

-- 或 年度分区：创建未来 3 年
CALL sp_create_yearly_partitions('sys_access_log', 'your_schema', 3);
CALL sp_create_yearly_partitions('sys_error_log', 'your_schema', 3);
CALL sp_create_yearly_partitions('sys_operation_log', 'your_schema', 3);
```

**步骤 4** — 启用 Event Scheduler 自动创建分区：

```sql
SET GLOBAL event_scheduler = ON;
```

然后取消 `partition-maintenance.sql` 底部对应 Event 定义的注释，将 `your_schema` 替换为实际库名。

#### 场景 C：运行中迁移（从不分区 → 分区）

对于已在运行的非分区表，使用 `alter-to-partition.sql` 原地转换。**无需停机**，已有数据自动分配到对应分区。

**步骤 1** — 确认 `create_time` 无 NULL 值：

```sql
SELECT 'sys_access_log' AS table_name, COUNT(*) AS null_count
FROM sys_access_log WHERE create_time IS NULL
UNION ALL
SELECT 'sys_error_log', COUNT(*) FROM sys_error_log WHERE create_time IS NULL
UNION ALL
SELECT 'sys_operation_log', COUNT(*) FROM sys_operation_log WHERE create_time IS NULL;
```

**步骤 2** — 修改主键（Online DDL，不停机）：

```sql
ALTER TABLE sys_access_log    DROP PRIMARY KEY, ADD PRIMARY KEY (id, create_time);
ALTER TABLE sys_error_log     DROP PRIMARY KEY, ADD PRIMARY KEY (id, create_time);
ALTER TABLE sys_operation_log DROP PRIMARY KEY, ADD PRIMARY KEY (id, create_time);
```

> 数据量特别大时，可考虑使用 `pt-online-schema-change`。

**步骤 3** — 添加分区（选择月度或年度，参见 `alter-to-partition.sql`）。

**步骤 4** — 部署维护存储过程并生成未来分区（同场景 B 步骤 2-4）。

#### 分区维护

存储过程 `sp_create_monthly_partitions` / `sp_create_yearly_partitions` 通过 `REORGANIZE PARTITION p_future` 从兜底分区中拆分出新分区。推荐的 Event Scheduler 配置：

- 月度分区：每天执行，提前创建 3 个月
- 年度分区：每月执行，提前创建 2 年

#### 过期分区清理（可选）

| 存储过程                        | 行为                                                                   |
| ------------------------------- | ---------------------------------------------------------------------- |
| `sp_drop_expired_partitions`    | 直接删除超过 N 天的分区。速度快，立即释放磁盘。**不可回滚。**          |
| `sp_archive_expired_partitions` | 先将数据复制到 `{表名}_archive` 归档表，再删除分区。需提前创建归档表。 |

> 不清理过期分区**不会**影响时间范围查询的性能（partition pruning 仍然生效）。主要影响是磁盘空间占用。

---

<a name="日本語"></a>

## 日本語

本プロジェクトは、**Spring Boot 4 + Java 25** をベースにしたエンタープライズグレードのバックエンドフレームワークであり、フロントエンドアプリケーションに対してユーザー認証、認可、およびデータ管理機能を提供します。

### アーキテクチャと技術スタック

- **アーキテクチャ**: フロントエンドとバックエンドの分離アーキテクチャ
- **認証**: Spring Security + JWT を使用したステートレス認証
- **ORM**: MyBatisPlus
- **データベース**: MySQL
- **キャッシュ & トークン管理**: Redis

### 主要な設計目標

- **統一レスポンス形式**: 一貫性のある API レスポンス構造
- **包括的なログシステム**: アクセスログ、例外ログ、操作ログをスタックトレース ID で関連付け
- **非同期ログ書き込み**: 非同期ログ書き込みによりシステムパフォーマンスを確保
- **セキュリティ保護**: CSRF 保護、CORS 設定、パスワード暗号化
- **マルチ環境サポート**: 開発、テスト、本番環境のサポート

### デプロイ時の注意事項：リバースプロキシ設定（重要）

本プロジェクトは `X-Forwarded-For` 等のプロキシヘッダーを使用して実際のクライアントIPアドレスを取得します。影響範囲は**レート制限**（`RateLimitFilter`）と**アクセスログ**（`AccessLogInterceptor`）です。

リバースプロキシがこれらのヘッダーを正しく上書きしない場合、クライアントが `X-Forwarded-For` を偽装してレート制限を回避したり、アクセスログを汚染する可能性があります。

#### Nginx 設定例

```nginx
server {
    listen 80;
    server_name example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;

        # 実際のTCPピアアドレスでX-Forwarded-Forを上書き（クライアントによるIP偽装を防止）
        proxy_set_header X-Forwarded-For $remote_addr;

        # 実際のクライアントIPを渡す
        proxy_set_header X-Real-IP $remote_addr;

        # 元のHostヘッダーを渡す
        proxy_set_header Host $host;

        # プロトコルタイプを渡す
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> **重要ポイント**：`X-Forwarded-For` の設定には `$remote_addr` を使用してください（`$proxy_add_x_forwarded_for` ではなく）。`$proxy_add_x_forwarded_for` は既存のヘッダーに追加するため、クライアントが偽装した値が保持されます。

> 多段プロキシ構成（CDN → ロードバランサー → Nginx → アプリ）の場合、最外層のプロキシのみが `X-Forwarded-For` を設定し、内層のプロキシはそのまま転送するようにしてください。

### デプロイ時の注意事項：Prometheus + Grafana モニタリング

本プロジェクトは Spring Boot Actuator と Micrometer Prometheus Registry を統合しています。管理エンドポイントは独立ポート（`9090`）で動作し、ビジネスポート（`8080`）と分離されています。

#### 環境別エンドポイント公開設定

| 環境 | 公開エンドポイント                                                   |
| ---- | -------------------------------------------------------------------- |
| dev  | health, info, prometheus, metrics, beans, env, configprops, mappings |
| test | health, info, prometheus, metrics                                    |
| prod | health, info, prometheus                                             |

#### セキュリティ：ネットワーク分離

管理ポート `9090` はインターネットに公開してはいけません。ファイアウォールで Prometheus サーバーの IP のみアクセスを許可してください。

```bash
# iptables 例：Prometheus サーバー（192.168.1.100）のみ 9090 ポートへのアクセスを許可
iptables -A INPUT -p tcp --dport 9090 -s 192.168.1.100 -j ACCEPT
iptables -A INPUT -p tcp --dport 9090 -j DROP
```

#### Docker Compose デプロイ（Prometheus + Alertmanager + Grafana）

```yaml
version: "3.8"
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9091:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/alert-rules.yml:/etc/prometheus/alert-rules.yml
      - ./monitoring/targets:/etc/prometheus/targets
      - prometheus_data:/prometheus
    command:
      - "--config.file=/etc/prometheus/prometheus.yml"
      - "--storage.tsdb.retention.time=15d"
    restart: unless-stopped

  alertmanager:
    image: prom/alertmanager:latest
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD:-admin}
    volumes:
      - grafana_data:/var/lib/grafana
    restart: unless-stopped

volumes:
  prometheus_data:
  grafana_data:
```

#### Prometheus 設定（`monitoring/prometheus.yml`）

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert-rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: ["alertmanager:9093"]

scrape_configs:
  - job_name: "spring-boot-app"
    metrics_path: "/actuator/prometheus"
    file_sd_configs:
      - files:
          - "/etc/prometheus/targets/*.json"
        refresh_interval: 30s
```

#### サービスディスカバリターゲット（`monitoring/targets/app.json`）

```json
[
  {
    "targets": ["192.168.1.1:9090", "192.168.1.2:9090", "192.168.1.3:9090"],
    "labels": { "env": "production", "app": "backend-base" }
  }
]
```

実際のデプロイ環境に合わせて IP アドレスとポートを変更してください。

#### アラートルール（`monitoring/alert-rules.yml`）

```yaml
groups:
  - name: spring-boot-alerts
    rules:
      - alert: InstanceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "インスタンス {{ $labels.instance }} がダウンしています"

      - alert: HighResponseTime
        expr: http_server_requests_seconds_max{uri!~"/actuator.*"} > 5
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "エンドポイント {{ $labels.uri }} のレスポンス時間が 5 秒を超えています"

      - alert: HighHeapUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "インスタンス {{ $labels.instance }} のヒープ使用率が 90% を超えています"
```

#### Alertmanager メール通知設定（`monitoring/alertmanager.yml`）

```yaml
global:
  smtp_smarthost: "smtp.example.com:587"
  smtp_from: "alertmanager@example.com"
  smtp_auth_username: "alertmanager@example.com"
  smtp_auth_password: "your-smtp-password"
  smtp_require_tls: true

route:
  receiver: "email-notifications"
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: "email-notifications"
    email_configs:
      - to: "ops-team@example.com"
        send_resolved: true
```

SMTP 設定を実際のメールサーバー情報に置き換えてください。

#### Grafana ダッシュボード設定

Grafana 起動後（デフォルト：`http://localhost:3000`、初期アカウント admin/admin）：

1. Prometheus データソースを追加：URL に `http://prometheus:9090` を入力
2. コミュニティダッシュボードを ID でインポート：
   - `4701` — JVM Micrometer ダッシュボード
   - `12900` — Spring Boot 統計ダッシュボード
   - `1860` — Node Exporter システムレベルメトリクスダッシュボード

追加のダッシュボードは Grafana UI からいつでもインポート可能です（再起動不要）。

### デプロイ時の注意事項：ログテーブルパーティション戦略

本プロジェクトは、3つのシステムログテーブル（`sys_access_log`、`sys_error_log`、`sys_operation_log`）に対してオプションの MySQL RANGE パーティションをサポートしています。

パーティショニングにより、**partition pruning**（MySQL が無関係なパーティションを自動的にスキップ）で時間範囲クエリのパフォーマンスが向上し、効率的なデータライフサイクル管理（行単位の DELETE ではなくパーティション全体を DROP）が可能になります。

#### ファイル一覧

パーティション関連の SQL ファイルは `src/main/resources/db/partition/` にあります：

| ファイル                    | 説明                                                                                                          |
| --------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `partition-monthly.sql`     | 月次パーティション DDL — 月単位でパーティションテーブルを作成                                                 |
| `partition-yearly.sql`      | 年次パーティション DDL — 年単位でパーティションテーブルを作成                                                 |
| `partition-maintenance.sql` | ストアドプロシージャ + Event Scheduler — 将来のパーティション自動作成と期限切れパーティションのクリーンアップ |
| `alter-to-partition.sql`    | オンラインマイグレーションスクリプト — 既存の非パーティションテーブルをパーティションテーブルに変換           |

> 非パーティション DDL は `src/main/resources/db/system.sql` にあります。

#### 戦略の選択

| 戦略                               | 適用シーン                                              | パーティションキー     |
| ---------------------------------- | ------------------------------------------------------- | ---------------------- |
| パーティションなし（`system.sql`） | 低トラフィック、シンプルなデプロイ                      | なし                   |
| 月次（`partition-monthly.sql`）    | 大量ログ（日次 10万件以上）、月次アーカイブが必要       | `TO_DAYS(create_time)` |
| 年次（`partition-yearly.sql`）     | 中程度のログ量（日次 10万件未満）、年次アーカイブで十分 | `YEAR(create_time)`    |

#### シナリオ A：新規デプロイ（パーティションなし）

標準 DDL を使用。追加手順は不要です。

```sql
SOURCE src/main/resources/db/system.sql;
```

#### シナリオ B：新規デプロイ（パーティションあり）

**ステップ 1** — パーティションテーブルを作成（いずれかを選択）：

```sql
-- 月次パーティション
SOURCE src/main/resources/db/partition/partition-monthly.sql;

-- または 年次パーティション
SOURCE src/main/resources/db/partition/partition-yearly.sql;
```

**ステップ 2** — メンテナンス用ストアドプロシージャをデプロイ：

```sql
SOURCE src/main/resources/db/partition/partition-maintenance.sql;
```

**ステップ 3** — 将来のパーティションを生成（`your_schema` を実際のデータベース名に置換）：

```sql
-- 月次：今後 6 ヶ月分を作成
CALL sp_create_monthly_partitions('sys_access_log', 'your_schema', 6);
CALL sp_create_monthly_partitions('sys_error_log', 'your_schema', 6);
CALL sp_create_monthly_partitions('sys_operation_log', 'your_schema', 6);

-- または 年次：今後 3 年分を作成
CALL sp_create_yearly_partitions('sys_access_log', 'your_schema', 3);
CALL sp_create_yearly_partitions('sys_error_log', 'your_schema', 3);
CALL sp_create_yearly_partitions('sys_operation_log', 'your_schema', 3);
```

**ステップ 4** — Event Scheduler を有効化してパーティションを自動作成：

```sql
SET GLOBAL event_scheduler = ON;
```

その後、`partition-maintenance.sql` の末尾にある対応する Event 定義のコメントを解除し、`your_schema` をデータベース名に置換してください。

#### シナリオ C：稼働中のシステムをマイグレーション（パーティションなし → あり）

既に非パーティションテーブルで稼働中のシステムには、`alter-to-partition.sql` を使用してインプレース変換します。**ダウンタイム不要**で、既存データは自動的に対応するパーティションに分配されます。

**ステップ 1** — `create_time` に NULL 値がないことを確認：

```sql
SELECT 'sys_access_log' AS table_name, COUNT(*) AS null_count
FROM sys_access_log WHERE create_time IS NULL
UNION ALL
SELECT 'sys_error_log', COUNT(*) FROM sys_error_log WHERE create_time IS NULL
UNION ALL
SELECT 'sys_operation_log', COUNT(*) FROM sys_operation_log WHERE create_time IS NULL;
```

**ステップ 2** — 主キーを変更（Online DDL、ダウンタイムなし）：

```sql
ALTER TABLE sys_access_log    DROP PRIMARY KEY, ADD PRIMARY KEY (id, create_time);
ALTER TABLE sys_error_log     DROP PRIMARY KEY, ADD PRIMARY KEY (id, create_time);
ALTER TABLE sys_operation_log DROP PRIMARY KEY, ADD PRIMARY KEY (id, create_time);
```

> 非常に大きなテーブルの場合は、`pt-online-schema-change` の使用を検討してください。

**ステップ 3** — パーティションを追加（月次または年次を選択、`alter-to-partition.sql` を参照）。

**ステップ 4** — メンテナンスをデプロイし将来のパーティションを生成（シナリオ B のステップ 2-4 と同じ）。

#### パーティションメンテナンス

ストアドプロシージャ `sp_create_monthly_partitions` / `sp_create_yearly_partitions` は `REORGANIZE PARTITION p_future` を使用してキャッチオールパーティションから新しいパーティションを分割します。推奨 Event Scheduler 設定：

- 月次パーティション：毎日実行、3 ヶ月先まで作成
- 年次パーティション：毎月実行、2 年先まで作成

#### 期限切れパーティションのクリーンアップ（オプション）

| プロシージャ                    | 動作                                                                                               |
| ------------------------------- | -------------------------------------------------------------------------------------------------- |
| `sp_drop_expired_partitions`    | N 日以上経過したパーティションを直接削除。高速でディスクを即時解放。**ロールバック不可。**         |
| `sp_archive_expired_partitions` | データを `{テーブル名}_archive` テーブルにコピーしてから削除。アーカイブテーブルの事前作成が必要。 |

> 期限切れパーティションをクリーンアップしなくても、時間範囲クエリのパフォーマンスには**影響しません**（partition pruning は引き続き機能します）。主な影響はディスク容量の消費です。
