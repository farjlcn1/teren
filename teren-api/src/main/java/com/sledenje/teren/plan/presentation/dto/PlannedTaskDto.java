package com.sledenje.teren.plan.presentation.dto;

import com.sledenje.teren.common.domain.model.WorkOrderType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Planiran nalog (vozilo) v dogodku")
public record PlannedTaskDto(
        String id,
        String vehiclePlate,
        @Schema(nullable = true) WorkOrderType type,
        @Schema(nullable = true) String note,
        @Schema(description = "Opravljen delovni nalog; null, dokler ni opravljen", nullable = true) String workOrderId) {
}
