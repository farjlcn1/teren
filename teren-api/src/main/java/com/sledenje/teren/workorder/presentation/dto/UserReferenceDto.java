package com.sledenje.teren.workorder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uporabnik (ID in ime)")
public record UserReferenceDto(String id, String fullName) {
}
