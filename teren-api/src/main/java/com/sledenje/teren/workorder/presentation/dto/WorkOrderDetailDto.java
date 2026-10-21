package com.sledenje.teren.workorder.presentation.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Polja naloga so v JSON na prvem nivoju (kot pri {@link WorkOrderDto}), dodana sta seznama fotografij in urejanj.
 */
@Schema(description = "Delovni nalog s fotografijami (po času nalaganja) in zgodovino urejanj (najnovejše prvo)")
public record WorkOrderDetailDto(
        @JsonUnwrapped WorkOrderDto workOrder,
        List<WorkOrderPhotoDto> photos,
        List<WorkOrderEditDto> edits) {
}
