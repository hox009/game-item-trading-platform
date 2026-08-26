package com.gametrade.user.dto;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String role
) {
}
