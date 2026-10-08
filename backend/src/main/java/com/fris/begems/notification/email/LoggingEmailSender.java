package com.fris.begems.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("Email delivery not configured (app.mail.provider=log); would send \"{}\" to {}",
                message.subject(), message.toAddress());
    }
}
