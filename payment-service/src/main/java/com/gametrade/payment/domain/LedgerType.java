package com.gametrade.payment.domain;

/**
 * Ledger movement types for auditability.
 */
public enum LedgerType {
    RECHARGE,   // user tops up
    DEBIT,      // buyer pays for an order
    CREDIT      // seller receives escrowed funds
}
