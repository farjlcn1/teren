package com.sledenje.teren.workorder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Fotografije in podpis, shranjeni po vnosu naloga")
public record WorkOrderAttachmentsRequest(
        @Schema(description = "Pot do slike podpisa; podpis nalog zaklene", nullable = true) String signatureUrl,
        @NotNull List<@Valid PhotoRequest> photos) {
}
