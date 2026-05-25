package com.rodin.SsuBench.Controller.Request;

import com.rodin.SsuBench.Validation.PrintableAscii;
import com.rodin.SsuBench.Validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank(message = "Имя пользователя не может быть пустым")
        @Size(min = 5, max = 32, message = "Имя пользователя должно быть от 5 до 32 символов")
        @PrintableAscii(message = "Имя пользователя должно содержать только ASCII символы")
        String username,

        @NotBlank(message = "Пароль не может быть пустым")
        @PrintableAscii(message = "Пароль должен содержать только ASCII символы")
        @StrongPassword
        String password
) {}
