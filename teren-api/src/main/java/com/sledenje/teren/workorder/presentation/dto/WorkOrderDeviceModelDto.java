package com.sledenje.teren.workorder.presentation.dto;

import com.sledenje.teren.workorder.domain.model.DeviceModel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Model naprave")
public record WorkOrderDeviceModelDto(String id, DeviceModel deviceModel, @Schema(nullable = true) String comment) {
}
