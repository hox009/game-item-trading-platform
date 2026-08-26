package com.gametrade.common.web;

/**
 * Header names populated by the gateway after authenticating the JWT.
 * Downstream services read identity from these instead of re-parsing tokens.
 */
public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    private GatewayHeaders() {
    }
}
