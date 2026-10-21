package com.sledenje.teren.common.domain.model;

/**
 * Vrsta posega (pri nalogu in pri planiranem nalogu). Vrednosti se ujemajo s Postgres enumom "WorkOrderType".
 */
public enum WorkOrderType {
    MONTAZA,
    DEMONTAZA,
    INTERVENCIJA,
    PREMONTAZA,
    OSTALO
}
