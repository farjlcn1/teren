package com.sledenje.teren.workorder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Nov delovni nalog.")
public record CreateWorkOrderRequest(
        @NotNull(message = "Manjka vsebina naloga.") @Valid WorkOrderDetailsRequest details,
        @NotNull List<@Valid DeviceModelRequest> deviceModels,
        @Schema(description = "Uporabnik, ki je nalog izpolnil") @NotBlank String createdById,
        @Schema(description = "Planirani nalog, iz katerega nalog nastaja", nullable = true) String plannedTaskId) {
}
