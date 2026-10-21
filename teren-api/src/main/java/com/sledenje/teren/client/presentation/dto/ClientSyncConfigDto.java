package com.sledenje.teren.client.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Nastavitve sinhronizacije strank. Žeton zunanjega sistema se nikoli ne vrne.")
public record ClientSyncConfigDto(
        @Schema(description = "URL zunanjega sistema", example = "https://crm.example.com/api/stranke", nullable = true)
        String apiUrl,
        @Schema(description = "Ali je žeton za zunanji sistem nastavljen")
        boolean hasToken,
        @Schema(description = "Čas zadnje sinhronizacije (UTC)", nullable = true)
        Instant lastSyncAt,
        @Schema(description = "Izid zadnje sinhronizacije", example = "V redu — 12 strank", nullable = true)
        String lastSyncStatus) {
}
