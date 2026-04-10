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
