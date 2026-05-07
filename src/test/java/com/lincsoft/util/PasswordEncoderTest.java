package com.lincsoft.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test class for generating and verifying BCrypt password hashes.
 *
 * <p>This class does not depend on the Spring context. It uses {@link BCryptPasswordEncoder}
 * directly (the same implementation and default strength 10 as {@code
 * SecurityConfig#passwordEncoder()}).
 *
 * <p>Usage (run from the project root):
 *
 * <ul>
 *   <li>Generate a hash for a custom password:
 *       <pre>
 *       mvn -Dtest=PasswordEncoderTest#generateHashForCustomPassword -Dpassword=yourPassword test
 *       </pre>
 * </ul>
 *
 * @author 林创科技
 * @since 2026-05-07
 */
class PasswordEncoderTest {

  /** Consistent with {@code SecurityConfig#passwordEncoder()}: BCrypt default strength 10. */
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  /** Default password (consistent with the comment in master.sql). */
  private static final String DEFAULT_PASSWORD = "admin123";

  @Test
  @DisplayName("Generate BCrypt hash for a custom password (specified via -Dpassword=xxx)")
  void generateHashForCustomPassword() {
    String rawPassword = System.getProperty("password");
    if (rawPassword == null || rawPassword.isBlank()) {
      rawPassword = DEFAULT_PASSWORD;
      System.out.println(
          "[Notice] -Dpassword parameter not specified, using default value admin123");
    }
    String hash = passwordEncoder.encode(rawPassword);
    printHashReport(rawPassword, hash);

    assertTrue(
        passwordEncoder.matches(rawPassword, hash),
        "Self-check of the generated BCrypt hash failed");
  }

  /** Print the hash report to the console. */
  private void printHashReport(String rawPassword, String hash) {
    System.out.println();
    System.out.println("========== BCrypt Hash Generation Result ==========");
    System.out.println("Raw Password : " + rawPassword);
    System.out.println("BCrypt Hash  : " + hash);
    System.out.println("===================================================");
    System.out.println();
  }
}
