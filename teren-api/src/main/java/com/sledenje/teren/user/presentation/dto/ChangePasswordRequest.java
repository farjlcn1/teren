package com.sledenje.teren.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Novo geslo uporabnika")
public record ChangePasswordRequest(
        @Schema(description = "Geslo: vsaj 8 znakov, velika črka, številka in poseben znak")
        @NotBlank(message = "Geslo mora imeti vsaj 8 znakov")
        String password) {
}
