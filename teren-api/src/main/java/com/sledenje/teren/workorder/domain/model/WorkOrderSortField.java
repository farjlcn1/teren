package com.sledenje.teren.workorder.domain.model;

/**
 * Polja, po katerih se da urediti seznam nalogov (skalarna polja naloga in ime stranke).
 */
public enum WorkOrderSortField {
    ORDER_DATE,
    IDENT,
    VEHICLE_PLATE,
    IMEI,
    CLIENT_NAME,
    TYPE,
    DIFFICULTY,
    CULPRIT
}
