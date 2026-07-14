package com.joaocarlos.kitchen_service.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectOrderRequest(
        @NotBlank String reason
) {}
