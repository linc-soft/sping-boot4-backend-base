package com.lincsoft.util;

import com.lincsoft.constant.CommonConstants;
import com.lincsoft.dto.AuthenticatedUserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT utility class for token generation, validation, and parsing.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Slf4j
public final class JwtUtil {

  private JwtUtil() {
    throw new AssertionError("Utility class: instantiation not allowed");
  }

  /**
   * Generate a JWT token with a unique JTI (JWT ID) for revocation support.
   *
   * @param subject The subject of the token, typically the user ID.
   * @param user The user object to be included in the token's payload.
   * @param secret The secret key used to sign the token.
   * @param expiration The expiration time of the token in milliseconds.
   * @return The generated JWT token.
   */
  public static String generateToken(
      String subject, AuthenticatedUserDTO user, String secret, long expiration) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(subject)
        .claim(CommonConstants.JWT_CLAIM_USER_KEY, user)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey(secret), Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Parse the JWT token and return the claims.
   *
   * @param token The JWT token to parse.
   * @param secret The secret key used to verify the token's signature.
   * @return The claims contained in the token.
   */
  public static Claims parseToken(String token, String secret) {
    return Jwts.parser()
        .verifyWith(getSigningKey(secret))
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * Get the signing key for the JWT.
   *
   * @param secret The secret key used to sign the JWT.
   * @return The signing key.
   */
  private static SecretKey getSigningKey(String secret) {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
}
