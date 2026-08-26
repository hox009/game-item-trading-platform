package com.gametrade.gateway.config;

import com.gametrade.common.security.JwtUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds jwt.* properties and exposes a shared {@link JwtUtil} bean.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Shared HS256 secret, must match user-service. At least 32 bytes. */
    private String secret = "change-me-please-use-a-32-byte-secret-key!!";

    /** Token validity in milliseconds (default 2 hours). */
    private long expirationMillis = 7_200_000L;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(secret, expirationMillis);
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    public void setExpirationMillis(long expirationMillis) {
        this.expirationMillis = expirationMillis;
    }
}
