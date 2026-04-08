package com.lincsoft.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties class.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  /**
   * JWT 設定。
   *
   * <p>アクセストークンおよびリフレッシュトークンの生成・検証に使用する設定値。
   */
  private Jwt jwt = new Jwt();

  /**
   * CORS 設定。
   *
   * <p>クロスオリジンリクエストを許可するオリジンの設定。
   */
  private Cors cors = new Cors();

  /**
   * JWT 設定の内部クラス。
   *
   * <p>{@code app.jwt} プレフィックス配下のプロパティをバインドする。
   */
  @Data
  public static class Jwt {

    /**
     * HMAC-SHA256 署名用の秘密鍵。
     *
     * <p>本番環境では環境変数またはシークレット管理ツールで上書きすること。
     */
    private String secret = "";

    /**
     * アクセストークンの有効期限（ミリ秒）。
     *
     * <p>デフォルト: 10分（600,000ms）
     */
    private long expiration = 600000L;

    /**
     * リフレッシュトークンの有効期限（ミリ秒）。
     *
     * <p>デフォルト: 1日（86,400,000ms）
     */
    private long refreshExpiration = 86400000L;
  }

  /**
   * CORS 設定の内部クラス。
   *
   * <p>{@code app.cors} プレフィックス配下のプロパティをバインドする。
   */
  @Data
  public static class Cors {

    /**
     * CORS 許可オリジン（カンマ区切りで複数指定可能）。
     *
     * <p>例: {@code http://localhost:5173,https://www.example.com}
     */
    private String allowedOrigins = "http://localhost:5173";
  }
}
