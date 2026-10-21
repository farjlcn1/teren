package com.sledenje.teren.client.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Stranke iz uvožene datoteke (izvor IMPORT)")
public record ImportClientsRequest(
        @Schema(description = "Stranke za uvoz; shranijo se vse ali nobena")
        @NotNull(message = "Seznam strank je obvezen")
        List<@Valid CreateClientRequest> clients) {
}
