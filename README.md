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
