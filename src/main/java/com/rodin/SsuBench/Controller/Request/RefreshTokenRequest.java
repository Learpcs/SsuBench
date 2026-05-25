package com.rodin.SsuBench.Controller.Request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh токен не может быть пустым")
        String refreshToken
) {}
