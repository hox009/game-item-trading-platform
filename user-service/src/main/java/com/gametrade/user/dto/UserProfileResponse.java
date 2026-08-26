package com.gametrade.user.dto;

import com.gametrade.user.domain.User;

import java.math.BigDecimal;

public record UserProfileResponse(
        Long id,
        String username,
        String role,
        BigDecimal balance
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getBalance()
        );
    }
}
