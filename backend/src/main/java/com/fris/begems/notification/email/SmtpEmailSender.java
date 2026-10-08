package com.fris.begems.notification.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public SmtpEmailSender(JavaMailSender mailSender, String fromAddress, String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            mail.setFrom(fromName + " <" + fromAddress + ">");
        }
        mail.setTo(message.toAddress());
        mail.setSubject(message.subject());
        mail.setText(message.text());
        mailSender.send(mail);
    }
}
