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
