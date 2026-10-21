package com.sledenje.teren.plan.domain.model;

import com.sledenje.teren.common.domain.model.WorkOrderType;

/**
 * @param workOrderId opravljen delovni nalog ali {@code null}, če planirani nalog še ni opravljen
 */
public record PlannedTask(String id, String vehiclePlate, WorkOrderType type, String note, String workOrderId) {

    public boolean isCompleted() {
        return workOrderId != null;
    }

}
