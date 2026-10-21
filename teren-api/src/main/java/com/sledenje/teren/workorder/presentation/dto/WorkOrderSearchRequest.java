package com.sledenje.teren.workorder.presentation.dto;

import com.sledenje.teren.workorder.domain.model.Culprit;
import com.sledenje.teren.workorder.domain.model.DeviceModel;
import com.sledenje.teren.workorder.domain.model.Difficulty;
import com.sledenje.teren.workorder.domain.model.InstallerName;
import com.sledenje.teren.workorder.domain.model.OptionType;
import com.sledenje.teren.workorder.domain.model.WorkOrderType;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;

/**
 * Parametri poizvedbe za seznam nalogov. Prazen parameter ne omejuje.
 */
public record WorkOrderSearchRequest(
        @Parameter(description = "Najzgodnejši datum naloga, ISO 8601 (npr. 2026-10-20T22:00:00Z)") Instant from,
        @Parameter(description = "Najkasnejši datum naloga, ISO 8601") Instant to,
        @Parameter(description = "ID stranke") String clientId,
        @Parameter(description = "Del registrske številke, ne glede na velike in male črke") String vehiclePlate,
        @Parameter(description = "Del IMEI-ja") String imei,
        WorkOrderType type,
        Difficulty difficulty,
        @Parameter(description = "Nalog ima tega monterja") InstallerName installer,
        @Parameter(description = "Nalog ima to opcijo") OptionType option,
        @Parameter(description = "Nalog ima ta model naprave") DeviceModel deviceModel,
        Culprit culprit,
        @Parameter(description = "Samo nalogi tega uporabnika") String createdById,
        @Parameter(description = "orderDate (privzeto), ident, vehiclePlate, imei, client, type, difficulty, culprit")
        String sort,
        @Parameter(description = "asc ali desc (privzeto)") String dir,
        @Parameter(description = "Največ toliko nalogov; brez omejitve, če ni podano")
        @Min(value = 1, message = "limit mora biti vsaj 1")
        @Max(value = 10000, message = "limit je lahko največ 10000")
        Integer limit) {
}
