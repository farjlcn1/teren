package com.sledenje.teren.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Nov uporabnik; je aktiven in nima pravic")
public record CreateUserRequest(
        @Schema(description = "Email; shrani se z malimi črkami", example = "janez.novak@example.com")
        @NotBlank(message = "Vnesi veljaven email.")
        @Email(message = "Vnesi veljaven email.")
        String email,
        @Schema(description = "Ime in priimek", example = "Janez Novak")
        @NotBlank(message = "Vnesi ime in priimek.")
        String fullName,
        @Schema(description = "Geslo: vsaj 8 znakov, velika črka, številka in poseben znak")
        @NotBlank(message = "Geslo mora imeti vsaj 8 znakov")
        String password) {
}
