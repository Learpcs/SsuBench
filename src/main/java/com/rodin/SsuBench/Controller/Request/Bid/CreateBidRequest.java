package com.rodin.SsuBench.Controller.Request.Bid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBidRequest(
        @NotBlank(message = "Описание отклика не может быть пустым")
        @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
        String description
) {}
