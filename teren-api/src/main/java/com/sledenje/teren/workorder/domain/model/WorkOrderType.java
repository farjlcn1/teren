package com.sledenje.teren.workorder.domain.model;

/**
 * Vrsta posega. Vrednosti se ujemajo s Postgres enumom "WorkOrderType".
 */
public enum WorkOrderType {
    MONTAZA,
    DEMONTAZA,
    INTERVENCIJA,
    PREMONTAZA,
    OSTALO
}
