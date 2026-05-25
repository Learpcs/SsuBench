package com.rodin.SsuBench.Controller.Response.Bid;

import com.rodin.SsuBench.Entity.BidStatus;

import java.time.ZonedDateTime;

public record BidResponse(
        Long id,
        Long executorId,
        String executorUsername,
        Long taskId,
        String description,
        BidStatus status,
        ZonedDateTime createdAt
) {}
