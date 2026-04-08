package com.lincsoft.util;

import com.lincsoft.constant.CommonConstants;
import com.lincsoft.dto.AuthenticatedUserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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

  /**
   * Generate a JWT token.
   *
   * @param subject The subject of the token, typically the user ID.
   * @param user The user object to be included in the token's payload.
   * @param secret The secret key used to sign the token.
   * @param expiration The expiration time of the token in seconds.
   * @return The generated JWT token.
   */
  public static String generateToken(
      String subject, AuthenticatedUserDTO user, String secret, long expiration) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .subject(subject)
        .claim(CommonConstants.JWT_CLAIM_USER_KEY, user)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey(secret), Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Extract the subject (usually the user ID) from the JWT token.
   *
   * @param token The JWT token to parse.
   * @param secret The secret key used to verify the token's signature.
   * @return The subject typically the user ID.
   */
  public static String getSubject(String token, String secret) {
    try {
      Claims claims = parseToken(token, secret);
      return claims.getSubject();
    } catch (ExpiredJwtException e) {
      log.warn("The JWT token has expired: {}", e.getMessage());
      return e.getClaims().getSubject();
    } catch (JwtException e) {
      log.warn("JWT token is invalid: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Validate the JWT token.
   *
   * @param token The JWT token to validate.
   * @param secret The secret key used to verify the token's signature.
   * @param subject The subject (usually the user ID) to be matched.
   * @return true if the token is valid and the subject matches, false otherwise.
   */
  public static boolean validateToken(String token, String secret, String subject) {
    try {
      Claims claims = parseToken(token, secret);
      String tokenSubject = claims.getSubject();
      // The signature is valid ∧ not expired ∧ subject matches
      return tokenSubject != null && tokenSubject.equals(subject);
    } catch (ExpiredJwtException e) {
      log.warn("The JWT token has expired: {}", e.getMessage());
      return false;
    } catch (JwtException e) {
      log.warn("The JWT token is invalid: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Extract the current user from the JWT token.
   *
   * @param token The JWT token to parse.
   * @param secret The secret key used to verify the token's signature.
   * @return The current user.
   */
  public static AuthenticatedUserDTO getCurrentUser(String token, String secret) {
    try {
      Claims claims = parseToken(token, secret);
      return claims.get(CommonConstants.JWT_CLAIM_USER_KEY, AuthenticatedUserDTO.class);
    } catch (ExpiredJwtException e) {
      log.warn("The JWT token has expired: {}", e.getMessage());
      return e.getClaims().get(CommonConstants.JWT_CLAIM_USER_KEY, AuthenticatedUserDTO.class);
    } catch (JwtException e) {
      log.warn("Failed to parse the JWT token: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Parse the JWT token and return the claims.
   *
   * @param token The JWT token to parse.
   * @param secret The secret key used to verify the token's signature.
   * @return The claims contained in the token.
   */
  private static Claims parseToken(String token, String secret) {
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
