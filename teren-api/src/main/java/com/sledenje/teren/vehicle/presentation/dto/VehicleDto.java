package com.sledenje.teren.vehicle.presentation.dto;

import com.sledenje.teren.vehicle.domain.model.VehicleSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Vozilo (registrska številka)")
public record VehicleDto(
        @Schema(description = "ID vozila", example = "cmd8x2k0q0000abcd1234efgh")
        String id,
        @Schema(description = "Registrska številka (velike črke)", example = "LJ 12-ABC")
        String plate,
        @Schema(description = "Izvor zapisa", example = "MANUAL")
        VehicleSource source,
        @Schema(description = "ID v zunanjem sistemu (samo pri sinhronizaciji)", nullable = true)
        String externalId,
        @Schema(description = "Čas vnosa (UTC)", example = "2026-07-24T07:32:07.123Z")
        Instant createdAt,
        @Schema(description = "Čas zadnje spremembe (UTC)", example = "2026-07-24T07:32:07.123Z")
        Instant updatedAt) {
}
