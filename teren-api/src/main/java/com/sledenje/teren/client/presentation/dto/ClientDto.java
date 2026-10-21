package com.sledenje.teren.client.presentation.dto;

import com.sledenje.teren.client.domain.model.ClientSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Stranka")
public record ClientDto(
        @Schema(description = "ID stranke", example = "cmd8x2k0q0000abcd1234efgh")
        String id,
        @Schema(description = "Ime stranke", example = "Prevozi Novak d.o.o.")
        String name,
        @Schema(description = "Naslov", example = "Dunajska cesta 1, 1000 Ljubljana", nullable = true)
        String address,
        @Schema(description = "Kontaktni podatki", example = "Janez Novak, 041 123 456", nullable = true)
        String contactInfo,
        @Schema(description = "Izvor zapisa", example = "MANUAL")
        ClientSource source,
        @Schema(description = "ID v zunanjem sistemu (samo pri sinhronizaciji)", nullable = true)
        String externalId,
        @Schema(description = "Čas vnosa (UTC)", example = "2026-07-24T07:32:07.123Z")
        Instant createdAt,
        @Schema(description = "Čas zadnje spremembe (UTC)", example = "2026-07-24T07:32:07.123Z")
        Instant updatedAt) {
}
