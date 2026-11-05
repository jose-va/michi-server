package com.michi.reservas.dto;

import jakarta.validation.constraints.NotBlank;

public record PushUnsubscribeDTO(
        @NotBlank String endpoint) {
}
