package com.sledenje.teren.workorder.domain.model;

import java.util.List;

/**
 * @param plannedTaskId planirani nalog, iz katerega nalog nastaja, ali {@code null}
 */
public record NewWorkOrder(
        WorkOrderDetails details,
        List<DeviceModelEntry> deviceModels,
        String createdById,
        String plannedTaskId) {
}
