package com.sledenje.teren.vehicle.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Registrska številka za vnos ali spremembo vozila")
public record VehiclePlateRequest(
        @Schema(description = "Registrska številka; shrani se brez presledkov na robu in z velikimi črkami",
                example = "lj 12-abc")
        @NotBlank(message = "Registrska št. je obvezna")
        String plate) {
}
