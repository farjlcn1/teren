package com.sledenje.teren.client.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Povezava do zunanjega sistema za sinhronizacijo strank")
public record UpdateClientSyncConfigRequest(
        @Schema(description = "URL, ki z GET vrne JSON seznam strank; prazen izbriše povezavo in žeton",
                example = "https://crm.example.com/api/stranke", nullable = true)
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Vnesi veljaven URL")
        String apiUrl,
        @Schema(description = "Bearer žeton; prazen obdrži obstoječega", nullable = true)
        String apiToken) {
}
