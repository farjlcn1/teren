package com.sledenje.teren.plan.domain.model;

import com.sledenje.teren.common.domain.model.WorkOrderType;

/**
 * Planiran nalog (vozilo) znotraj dogodka.
 *
 * @param type        pričakovana vrsta posega ali {@code null}
 * @param workOrderId opravljen delovni nalog; {@code null}, dokler nalog ni opravljen
 */
public record NewPlannedTask(String vehiclePlate, WorkOrderType type, String note, String workOrderId) {
}
