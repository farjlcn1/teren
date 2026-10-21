package com.sledenje.teren.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Podatki za prijavo")
public record LoginRequest(
        @NotBlank(message = "Vnesi veljaven email in geslo.")
        String email,
        @NotBlank(message = "Vnesi veljaven email in geslo.")
        String password) {
}
