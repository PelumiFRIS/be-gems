package com.fris.begems.security;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/** One-time passwords shown once to an admin, who shares them with the user directly. */
@Component
public class TemporaryPasswordGenerator {

    private static final int TEMPORARY_PASSWORD_BYTES = 18;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TEMPORARY_PASSWORD_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
