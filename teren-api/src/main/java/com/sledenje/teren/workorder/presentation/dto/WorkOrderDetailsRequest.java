package com.sledenje.teren.workorder.presentation.dto;

import com.sledenje.teren.common.domain.model.WorkOrderType;
import com.sledenje.teren.workorder.domain.model.Culprit;
import com.sledenje.teren.workorder.domain.model.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Skupna vsebina vnosa in urejanja naloga. Pravila med polji (npr. intervencija zahteva IMEI prej in krivca)
 * preveri domenski nivo in ob kršitvi vrne 422.
 */
@Schema(description = "Vsebina naloga")
public record WorkOrderDetailsRequest(
        @NotNull(message = "Izberi tip naloga.") WorkOrderType type,
        @NotNull(message = "Izberi zahtevnost.") Difficulty difficulty,
        @NotBlank(message = "Izberi stranko.") String clientId,
        @NotBlank(message = "Vnesi registrsko številko.") String vehiclePlate,
        @NotBlank(message = "Vnesi znamko vozila.") String vehicleBrand,
        @NotBlank(message = "Vnesi model vozila.") String vehicleModel,
        @NotNull(message = "Neveljavno leto.") Integer vehicleYear,
        @Schema(description = "Zadnjih 10 številk IMEI-ja") @NotBlank(message = "IMEI mora vsebovati natanko zadnjih 10 številk.") String imei,
        @Schema(description = "Zadnjih 10 številk prejšnjega IMEI-ja; obvezno pri intervenciji", nullable = true) String imeiPrev,
        @Schema(description = "Obvezno pri intervenciji", nullable = true) Culprit culprit,
        @Schema(nullable = true) String comment,
        @NotNull(message = "Izberi vsaj enega monterja.") List<@Valid InstallerRequest> installers,
        @NotNull List<@Valid OptionRequest> options) {
}
