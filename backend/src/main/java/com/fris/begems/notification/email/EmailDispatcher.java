package com.fris.begems.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends each email only after the triggering transaction commits (so a rolled-back
 * launch never emails anyone) and off the request thread (so a slow provider never
 * slows the user down). Failures are logged, never rethrown — board-portal's rule
 * that a notification must not fail the action that caused it.
 */
@Component
public class EmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    private final EmailSender emailSender;

    public EmailDispatcher(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void dispatch(EmailMessage message) {
        try {
            emailSender.send(message);
        } catch (RuntimeException e) {
            log.warn("Failed to send \"{}\" to {}", message.subject(), message.toAddress(), e);
        }
    }
}
