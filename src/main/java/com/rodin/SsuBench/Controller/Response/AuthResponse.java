package com.rodin.SsuBench.Controller.Response;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
