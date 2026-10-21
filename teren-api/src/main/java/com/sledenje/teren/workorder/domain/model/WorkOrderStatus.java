package com.sledenje.teren.workorder.domain.model;

/**
 * Stanje naloga. Vrednosti se ujemajo s Postgres enumom "WorkOrderStatus".
 */
public enum WorkOrderStatus {
    ACTIVE,
    CANCELLED
}
