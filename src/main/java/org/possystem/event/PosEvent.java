package org.possystem.event;

/**
 * Enum representing all possible POS system events.
 * These events are fired by PosEventDispatcher
 * and received by PosEventListener.
 */
public enum PosEvent {
    TRANSACTION_CREATED,
    ITEM_ADDED,
    ITEM_NOT_FOUND,
    ITEM_VOIDED,
    ITEM_UPDATED,
    QUANTITY_UPDATED,
    TRANSACTION_VOIDED,
    TRANSACTION_TOTALLED,
    PAYMENT_PROCESSED
}