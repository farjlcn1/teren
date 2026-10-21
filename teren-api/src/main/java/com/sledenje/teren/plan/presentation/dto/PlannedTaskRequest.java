package com.sledenje.teren.plan.presentation.dto;

import com.sledenje.teren.common.domain.model.WorkOrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Planiran nalog (vozilo)")
public record PlannedTaskRequest(
        @NotBlank(message = "Vnesi registrsko številko.") String vehiclePlate,
        @Schema(nullable = true) WorkOrderType type,
        @Schema(nullable = true) String note) {
}
