/**
 * Notification engine (memo §29): an in-app notification centre plus email copies.
 * Email goes through a pluggable EmailSender (Brevo HTTP API by default in
 * production, since Render's free tier blocks SMTP), sent after commit and off the
 * request thread; failures are logged rather than failing the request, as in
 * board-portal's EmailNotificationService.
 */
package com.fris.begems.notification;
