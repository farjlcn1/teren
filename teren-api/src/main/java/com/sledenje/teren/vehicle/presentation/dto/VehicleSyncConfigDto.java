package com.sledenje.teren.vehicle.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Nastavitve sinhronizacije vozil. Žeton zunanjega sistema se nikoli ne vrne.")
public record VehicleSyncConfigDto(
        @Schema(description = "URL zunanjega sistema", example = "https://fleet.example.com/api/vozila", nullable = true)
        String apiUrl,
        @Schema(description = "Ali je žeton za zunanji sistem nastavljen")
        boolean hasToken,
        @Schema(description = "Čas zadnje sinhronizacije (UTC)", nullable = true)
        Instant lastSyncAt,
        @Schema(description = "Izid zadnje sinhronizacije", example = "V redu — 12 registrskih št.", nullable = true)
        String lastSyncStatus) {
}
