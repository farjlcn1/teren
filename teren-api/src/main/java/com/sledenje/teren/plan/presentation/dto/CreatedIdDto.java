package com.sledenje.teren.plan.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ID ustvarjenega zapisa")
public record CreatedIdDto(String id) {
}
