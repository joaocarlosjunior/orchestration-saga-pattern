package com.joaocarlos.delivery_service.dto;

import jakarta.validation.constraints.NotBlank;

public record FailDeliveryRequest(
        @NotBlank String reason
) {}
