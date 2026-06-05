package com.lincsoft.event;

/**
 * Domain event published when a new user has been successfully created.
 *
 * <p>Listened to by {@code UserCreatedEventListener} with {@code @TransactionalEventListener(phase
 * = AFTER_COMMIT)} so that downstream side-effects (e.g., sending a welcome email) only occur after
 * the surrounding database transaction has been committed. If the transaction rolls back, the event
 * is discarded and no side-effects fire.
 *
 * <p>Carries the raw password because the welcome email is the only way to deliver the initial
 * credentials to the user; after the email is sent, the plaintext password is no longer recoverable
 * from the database.
 *
 * @param userId the newly created user ID
 * @param email the user's email address (used as the email recipient)
 * @param username the user's username (displayed in the email)
 * @param rawPassword the temporary plaintext password generated for the new user
 * @param loginUrl the login page URL included in the welcome email
 * @param locale the locale string (e.g., "en", "zh", "ja") used for email template i18n
 * @author 林创科技
 * @since 2026-06-05
 */
public record UserCreatedEvent(
    Long userId,
    String email,
    String username,
    String rawPassword,
    String loginUrl,
    String locale) {}
