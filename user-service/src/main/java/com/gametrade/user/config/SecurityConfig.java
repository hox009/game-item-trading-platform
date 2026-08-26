package com.gametrade.user.config;

import com.gametrade.common.security.JwtUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Exposes the shared {@link JwtUtil} (secret must match the gateway) and a
 * BCrypt password encoder.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class SecurityConfig {

    private String secret = "change-me-please-use-a-32-byte-secret-key!!";
    private long expirationMillis = 7_200_000L;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(secret, expirationMillis);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
