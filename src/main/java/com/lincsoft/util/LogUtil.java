package com.lincsoft.util;

import com.lincsoft.constant.CommonConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * ログ処理用共通ユーティリティクラス。
 *
 * <p>提供する機能:
 *
 * <ul>
 *   <li>{@link #truncate}: 指定された最大長で文字列を切り捨てる
 *   <li>{@link #getClientIp}: プロキシ対応のクライアントIP取得
 *   <li>{@link #buildRequestHeaders}: リクエストヘッダーをJSON文字列として構築（機密データマスク済み）
 *   <li>{@link #extractRequestBody}: リクエストボディを抽出（機密データマスク済み）
 *   <li>{@link #escapeJson}: JSON文字列のエスケープ処理
 * </ul>
 *
 * @author 林創科技
 * @since 2026-04-08
 */
@Slf4j
public final class LogUtil {

  /** リクエストボディの最大記録文字数 */
  private static final int MAX_REQUEST_BODY_LENGTH = 2000;

  /** マスク置換文字列 */
  private static final String MASKED_VALUE = "******";

  /** マスク対象のヘッダー名（小文字） */
  private static final String HEADER_AUTHORIZATION_LOWER = "authorization";

  /** マスク対象のボディフィールドパターン（パスワード関連） */
  private static final String PASSWORD_PATTERN = "password";

  private LogUtil() {
    // ユーティリティクラスのためインスタンス化を禁止
  }

  /**
   * 指定された最大長で文字列を切り捨てる。
   *
   * @param str 切り捨て対象の文字列
   * @param maxLen 最大長
   * @return 切り捨て後の文字列
   */
  public static String truncate(String str, int maxLen) {
    if (str == null) {
      return null;
    }
    if (str.length() <= maxLen) {
      return str;
    }
    return str.substring(0, maxLen) + CommonConstants.TRUNCATE_SUFFIX;
  }

  /**
   * プロキシ対応のクライアントIPアドレスを取得する。
   *
   * <p>以下のヘッダーを順に確認し、最初に有効な値を返す:
   *
   * <ol>
   *   <li>X-Forwarded-For（カンマ区切りの場合は最初のIPを使用）
   *   <li>X-Real-IP
   *   <li>{@code request.getRemoteAddr()}
   * </ol>
   *
   * @param request HTTPリクエスト
   * @return クライアントIPアドレス
   */
  public static String getClientIp(HttpServletRequest request) {
    // X-Forwarded-Forヘッダーを確認（リバースプロキシ経由の場合）
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
      // カンマ区切りの場合、最初のIPが実際のクライアントIP
      return ip.split(",")[0].trim();
    }
    // X-Real-IPヘッダーを確認（Nginx等のプロキシ）
    ip = request.getHeader("X-Real-IP");
    if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
      return ip.trim();
    }
    // フォールバック: サーブレットコンテナから直接取得
    return request.getRemoteAddr();
  }

  /**
   * リクエストヘッダーをJSON文字列として構築する。
   *
   * <p>Authorizationヘッダーの値はマスク処理される。
   *
   * @param request HTTPリクエスト
   * @return リクエストヘッダーのJSON文字列
   */
  public static String buildRequestHeaders(HttpServletRequest request) {
    try {
      StringJoiner joiner = new StringJoiner(",", "{", "}");
      Enumeration<String> headerNames = request.getHeaderNames();
      if (headerNames != null) {
        while (headerNames.hasMoreElements()) {
          String name = headerNames.nextElement();
          String value = request.getHeader(name);
          // Authorizationヘッダーをマスク処理
          if (HEADER_AUTHORIZATION_LOWER.equalsIgnoreCase(name)) {
            value = MASKED_VALUE;
          }
          joiner.add("\"" + escapeJson(name) + "\":\"" + escapeJson(value) + "\"");
        }
      }
      return joiner.toString();
    } catch (Exception e) {
      log.warn("リクエストヘッダーのJSON変換に失敗しました: {}", e.getMessage());
      return null;
    }
  }

  /**
   * リクエストボディを抽出する。
   *
   * <p>{@link ContentCachingRequestWrapper} の場合のみボディを読み取り可能。 パスワード関連フィールドが含まれる場合はマスク処理を行う。
   * 最大文字数を超える場合は切り捨てる。
   *
   * @param request HTTPリクエスト
   * @return リクエストボディ文字列。取得できない場合はnull
   */
  public static String extractRequestBody(HttpServletRequest request) {
    if (request instanceof ContentCachingRequestWrapper wrapper) {
      byte[] content = wrapper.getContentAsByteArray();
      if (content.length > 0) {
        String body = new String(content, StandardCharsets.UTF_8);
        // パスワード関連フィールドをマスク処理
        body = maskSensitiveFields(body);
        return truncate(body, MAX_REQUEST_BODY_LENGTH);
      }
    }
    return null;
  }

  /**
   * JSON文字列内の特殊文字をエスケープする。
   *
   * <p>エスケープ対象: バックスラッシュ、ダブルクォート、改行、復帰、タブ
   *
   * @param value エスケープ対象の文字列
   * @return エスケープ後の文字列。nullの場合は空文字列
   */
  public static String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  /**
   * 機密フィールド（パスワード等）をマスク処理する。
   *
   * <p>JSONボディ内の "password" を含むキーの値をマスクする。 簡易的な正規表現ベースの置換を使用。
   *
   * @param body リクエストボディ文字列
   * @return マスク処理後の文字列
   */
  private static String maskSensitiveFields(String body) {
    if (body == null) {
      return null;
    }
    // "xxxpasswordxxx":"value" パターンをマスク（大文字小文字無視）
    return body.replaceAll(
        "(?i)(\"[^\"]*" + PASSWORD_PATTERN + "[^\"]*\"\\s*:\\s*)\"[^\"]*\"",
        "$1\"" + MASKED_VALUE + "\"");
  }
}
