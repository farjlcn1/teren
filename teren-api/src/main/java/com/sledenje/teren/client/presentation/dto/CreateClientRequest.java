package com.sledenje.teren.client.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Podatki za vnos nove stranke")
public record CreateClientRequest(
        @Schema(description = "Ime stranke (obvezno)", example = "Prevozi Novak d.o.o.")
        @NotBlank(message = "Ime je obvezno")
        String name,
        @Schema(description = "Naslov", example = "Dunajska cesta 1, 1000 Ljubljana", nullable = true)
        String address,
        @Schema(description = "Kontaktni podatki", example = "Janez Novak, 041 123 456", nullable = true)
        String contactInfo) {
}
