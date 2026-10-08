package com.fris.begems.notification.email;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Brevo's transactional email API (https://developers.brevo.com/reference/sendtransacemail). */
public class BrevoEmailSender implements EmailSender {

    private static final String DEFAULT_BASE_URL = "https://api.brevo.com";

    private final RestClient restClient;
    private final String fromAddress;
    private final String fromName;

    public BrevoEmailSender(String apiKey, String fromAddress, String fromName) {
        this(DEFAULT_BASE_URL, apiKey, fromAddress, fromName);
    }

    BrevoEmailSender(String baseUrl, String apiKey, String fromAddress, String fromName) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("api-key", apiKey)
                .build();
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void send(EmailMessage message) {
        Map<String, Object> recipient = message.toName() == null
                ? Map.of("email", message.toAddress())
                : Map.of("email", message.toAddress(), "name", message.toName());
        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", fromAddress, "name", fromName),
                "to", List.of(recipient),
                "subject", message.subject(),
                "textContent", message.text());
        restClient.post()
                .uri("/v3/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
