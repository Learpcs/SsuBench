package com.rodin.SsuBench.Controller.Response.Task;

import com.rodin.SsuBench.Entity.TaskStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record TaskResponse(
        Long id,
        String description,
        Long customerId,
        String customerUsername,
        TaskStatus status,
        BigDecimal reward,
        Long acceptedBidId,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {}
