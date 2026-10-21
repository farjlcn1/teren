package com.sledenje.teren.workorder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Fotografija naloga; datoteka je v mapi z naloženimi datotekami aplikacije")
public record WorkOrderPhotoDto(
        String id,
        @Schema(description = "Pot znotraj mape z naloženimi datotekami") String filePath,
        @Schema(description = "Čas iz EXIF podatkov (UTC)", nullable = true) Instant takenAt,
        @Schema(description = "Čas nalaganja (UTC)") Instant uploadedAt) {
}
