package com.fris.begems.notification.email;

/**
 * Selected by {@code app.mail.provider}: "brevo" (HTTP API — works on Render's free
 * tier, which blocks outbound SMTP ports), "smtp" (JavaMailSender, for paid hosting
 * or local mail catchers), or "log" (default — writes the email to the log only).
 */
public interface EmailSender {

    void send(EmailMessage message);
}
