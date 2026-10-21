package com.sledenje.teren.workorder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Schema(description = "Fotografija, ki jo je aplikacija shranila na disk")
public record PhotoRequest(
        @Schema(description = "Pot znotraj mape z naloženimi datotekami") @NotBlank String filePath,
        @Schema(description = "Čas iz EXIF podatkov (UTC)", nullable = true) Instant takenAt) {
}
