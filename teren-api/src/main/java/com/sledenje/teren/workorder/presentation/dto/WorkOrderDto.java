package com.sledenje.teren.workorder.presentation.dto;

import com.sledenje.teren.common.domain.model.WorkOrderType;
import com.sledenje.teren.workorder.domain.model.Culprit;
import com.sledenje.teren.workorder.domain.model.Difficulty;
import com.sledenje.teren.workorder.domain.model.WorkOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Delovni nalog (brez fotografij in zgodovine urejanj)")
public record WorkOrderDto(
        String id,
        @Schema(description = "Številka naloga", example = "20261021-001") String ident,
        @Schema(description = "Datum in čas naloga (UTC)") Instant orderDate,
        WorkOrderType type,
        Difficulty difficulty,
        WorkOrderStatus status,
        WorkOrderClientDto client,
        String vehiclePlate,
        String vehicleBrand,
        String vehicleModel,
        int vehicleYear,
        String imei,
        @Schema(nullable = true) String imeiPrev,
        @Schema(nullable = true) Culprit culprit,
        @Schema(nullable = true) String comment,
        @Schema(description = "Pot do slike podpisa", nullable = true) String signatureUrl,
        @Schema(nullable = true) Instant signedAt,
        @Schema(description = "Čas zaklepa (ob podpisu); aplikacija ga prikaže kot \"Zaklenjen\"", nullable = true) Instant lockedAt,
        @Schema(description = "Zadnje pošiljanje po e-pošti", nullable = true) Instant sentAt,
        UserReferenceDto createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<WorkOrderInstallerDto> installers,
        List<WorkOrderOptionDto> options,
        List<WorkOrderDeviceModelDto> deviceModels) {
}
