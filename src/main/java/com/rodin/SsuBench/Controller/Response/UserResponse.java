package com.rodin.SsuBench.Controller.Response;

import com.rodin.SsuBench.Entity.UserRole;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record UserResponse(
        Long id,
        String username,
        UserRole role,
        BigDecimal balance,
        Boolean isBlocked,
        ZonedDateTime createdAt
) {}
