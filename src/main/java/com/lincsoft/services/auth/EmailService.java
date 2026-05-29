package com.lincsoft.services.auth;

import com.lincsoft.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Email service for sending transactional emails.
 *
 * <p>Uses Spring's {@link JavaMailSender} with Thymeleaf templates for HTML email rendering. All
 * send operations are asynchronous to avoid blocking the request thread.
 *
 * @author 林创科技
 * @since 2026-05-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final AppProperties appProperties;

  @Value("${spring.mail.username}")
  private String mailUsername;

  /**
   * Send a password reset email asynchronously.
   *
   * <p>Renders the email body from a Thymeleaf template and sends via SMTP. Failures are logged but
   * not rethrown to avoid exposing internal state to the caller.
   *
   * @param toEmail recipient email address
   * @param resetLink full password reset URL
   * @param expiresInMinutes token TTL in minutes (for display in the email)
   * @param locale locale string (e.g., "en", "zh", "ja") for template i18n
   */
  @Async("asyncExecutor")
  public void sendPasswordResetEmail(
      String toEmail, String resetLink, long expiresInMinutes, String locale) {
    try {
      String lang = locale != null ? locale : "en";
      // Extract base language (e.g., "zh-CN" → "zh") for reliable matching
      String baseLang = lang.contains("-") ? lang.substring(0, lang.indexOf("-")) : lang;
      Locale localeObj = Locale.of(baseLang);
      Context context = new Context(localeObj);
      context.setVariable("resetLink", resetLink);
      context.setVariable("expiresInMinutes", expiresInMinutes);

      String subject;
      if (baseLang.startsWith("zh")) {
        subject = "密码重置 - " + appProperties.getMail().getSenderName();
      } else if (baseLang.startsWith("ja")) {
        subject = "パスワードリセット - " + appProperties.getMail().getSenderName();
      } else {
        subject = "Password Reset - " + appProperties.getMail().getSenderName();
      }

      String htmlBody = templateEngine.process("email/password-reset", context);

      sendHtmlEmail(toEmail, subject, htmlBody);
      log.info("Password reset email sent to {}", maskEmail(toEmail));
    } catch (Exception e) {
      log.error(
          "Failed to send password reset email to {}: {}", maskEmail(toEmail), e.getMessage());
    }
  }

  /**
   * Send an HTML email with the given subject and body.
   *
   * @param to recipient email
   * @param subject email subject
   * @param htmlBody HTML email body
   * @throws jakarta.mail.MessagingException if the message cannot be composed
   */
  private void sendHtmlEmail(String to, String subject, String htmlBody)
      throws jakarta.mail.MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
    helper.setFrom(mailUsername);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(htmlBody, true);
    mailSender.send(message);
  }

  private String maskEmail(String email) {
    if (email == null || !email.contains("@")) {
      return email;
    }
    int atIndex = email.indexOf('@');
    if (atIndex <= 2) {
      return email.charAt(0) + "***" + email.substring(atIndex);
    }
    return email.substring(0, 2) + "***" + email.substring(atIndex);
  }
}
