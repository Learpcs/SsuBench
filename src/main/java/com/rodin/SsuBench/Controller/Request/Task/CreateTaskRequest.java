package com.rodin.SsuBench.Controller.Request.Task;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateTaskRequest(
        @NotBlank(message = "Описание задачи не может быть пустым")
        @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
        String description,

        @NotNull(message = "Награда не может быть пустой")
        @DecimalMin(value = "0.01", message = "Награда должна быть больше 0")
        BigDecimal reward
) {}
