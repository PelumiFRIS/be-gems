package com.fris.begems.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    /** Falls back to log-only when the chosen provider is missing its settings, so a bad config never blocks startup. */
    @Bean
    public EmailSender emailSender(@Value("${app.mail.provider:log}") String provider,
            @Value("${app.mail.brevo-api-key:}") String brevoApiKey,
            @Value("${app.mail.from-address:}") String fromAddress,
            @Value("${app.mail.from-name:BE-GEMS}") String fromName,
            ObjectProvider<JavaMailSender> javaMailSender) {
        switch (provider.trim().toLowerCase()) {
            case "brevo" -> {
                if (brevoApiKey.isBlank() || fromAddress.isBlank()) {
                    log.warn("app.mail.provider=brevo but BREVO_API_KEY or MAIL_FROM_ADDRESS is missing; "
                            + "emails will only be logged");
                    return new LoggingEmailSender();
                }
                log.info("Sending email through Brevo as {}", fromAddress);
                return new BrevoEmailSender(brevoApiKey, fromAddress, fromName);
            }
            case "smtp" -> {
                JavaMailSender mailSender = javaMailSender.getIfAvailable();
                if (mailSender == null) {
                    log.warn("app.mail.provider=smtp but no SMTP settings are configured; emails will only be logged");
                    return new LoggingEmailSender();
                }
                log.info("Sending email through SMTP");
                return new SmtpEmailSender(mailSender, fromAddress, fromName);
            }
            default -> {
                log.info("Email delivery is log-only (set MAIL_PROVIDER to enable it)");
                return new LoggingEmailSender();
            }
        }
    }
}
