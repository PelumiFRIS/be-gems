package com.fris.begems.notification.email;

/** Plain-text email to a single recipient. Published as an application event and sent after commit. */
public record EmailMessage(String toAddress, String toName, String subject, String text) {
}
