package com.gametrade.common.api;

/**
 * Business result codes shared across all services.
 * Range convention:
 *   0            success
 *   1000-1999    generic client errors
 *   2000-2999    auth / user
 *   3000-3999    catalog / item
 *   4000-4999    inventory
 *   5000-5999    order
 *   6000-6999    payment
 *   9000-9999    server / infrastructure
 */
public enum ResultCode {

    SUCCESS(0, "success"),

    BAD_REQUEST(1000, "bad request"),
    PARAM_INVALID(1001, "parameter validation failed"),
    NOT_FOUND(1002, "resource not found"),
    TOO_MANY_REQUESTS(1003, "too many requests"),

    UNAUTHORIZED(2000, "unauthorized"),
    TOKEN_INVALID(2001, "token invalid or expired"),
    USERNAME_EXISTS(2002, "username already exists"),
    LOGIN_FAILED(2003, "invalid username or password"),

    ITEM_NOT_FOUND(3000, "item not found"),
    ITEM_OFF_SHELF(3001, "item is off shelf"),

    STOCK_NOT_ENOUGH(4000, "stock not enough"),
    STOCK_FREEZE_FAILED(4001, "failed to freeze stock"),

    ORDER_NOT_FOUND(5000, "order not found"),
    ORDER_STATUS_ILLEGAL(5001, "illegal order status transition"),

    PAYMENT_FAILED(6000, "payment failed"),
    BALANCE_NOT_ENOUGH(6001, "balance not enough"),

    INTERNAL_ERROR(9000, "internal server error"),
    SERVICE_UNAVAILABLE(9001, "service temporarily unavailable");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
