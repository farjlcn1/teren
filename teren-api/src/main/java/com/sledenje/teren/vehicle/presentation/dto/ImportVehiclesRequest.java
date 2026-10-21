package com.sledenje.teren.vehicle.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Registrske številke iz uvožene datoteke (izvor IMPORT)")
public record ImportVehiclesRequest(
        @Schema(description = "Registrske številke; obstoječe in ponovljene se preskočijo", example = "[\"LJ 12-ABC\"]")
        @NotNull(message = "Seznam registrskih št. je obvezen")
        List<@NotBlank(message = "Registrska št. je obvezna") String> plates) {
}
