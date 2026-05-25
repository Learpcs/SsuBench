package com.rodin.SsuBench.Controller.Response;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record PaymentResponse(
        Long id,
        Long taskId,
        Long bidId,
        BigDecimal amount,
        ZonedDateTime processedAt
) {}
