package com.fris.begems.evaluation.dto;

/** {@code withoutPortalAccess}: outstanding respondents who couldn't be reminded because they haven't been invited. */
public record ReminderResult(int remindersSent, int withoutPortalAccess) {
}
