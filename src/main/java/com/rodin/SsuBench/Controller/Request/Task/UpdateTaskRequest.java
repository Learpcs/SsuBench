package com.rodin.SsuBench.Controller.Request.Task;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateTaskRequest(
        @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
        String description,

        @DecimalMin(value = "0.01", message = "Награда должна быть больше 0")
        BigDecimal reward
) {}
