package com.gametrade.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Role is optional and defaults to BUYER.
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 64)
        String username,

        @NotBlank
        @Size(min = 6, max = 64)
        String password,

        @Pattern(regexp = "BUYER|SELLER", message = "role must be BUYER or SELLER")
        String role
) {
}
