package com.lincsoft.event;

import com.lincsoft.services.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link UserCreatedEvent} and dispatches the welcome email after the surrounding
 * transaction commits.
 *
 * <p>By using {@code phase = AFTER_COMMIT}, this listener fires only when the database transaction
 * has been successfully committed. If the transaction rolls back (e.g., a later step in an outer
 * service fails after the user has been created), the event is discarded and no email is sent. This
 * prevents the previous inconsistency where a user creation appeared to succeed but a downstream
 * failure caused a rollback while the welcome email had already been dispatched.
 *
 * <p>The actual SMTP send is delegated to {@link EmailService#sendNewUserWelcomeEmail}, which is
 * annotated with {@code @Async("asyncExecutor")} and swallows exceptions internally.
 *
 * @author 林创科技
 * @since 2026-06-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedEventListener {

  private final EmailService emailService;

  /**
   * Handle the {@link UserCreatedEvent} after the publishing transaction commits.
   *
   * @param event the user-created event published from {@code UserService.createUser}
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserCreated(UserCreatedEvent event) {
    log.debug("Handling UserCreatedEvent for user id {}", event.userId());
    emailService.sendNewUserWelcomeEmail(
        event.email(), event.username(), event.rawPassword(), event.loginUrl(), event.locale());
  }
}
