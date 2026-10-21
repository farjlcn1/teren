package com.sledenje.teren.workorder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nova vsebina naloga (monterji in opcije se zamenjajo v celoti).")
public record UpdateWorkOrderRequest(
        @NotNull(message = "Manjka vsebina naloga.") @Valid WorkOrderDetailsRequest details,
        @Schema(description = "Uporabnik, ki ureja; zapiše se v zgodovino urejanj") @NotBlank String editedById) {
}
