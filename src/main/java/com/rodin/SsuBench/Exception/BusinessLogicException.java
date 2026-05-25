package com.rodin.SsuBench.Exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public class BusinessLogicException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String message;

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }

    public static BusinessLogicException of(ErrorCode errorCode, Object ... args) {
        return new BusinessLogicException(errorCode, String.format(errorCode.getMessage(), args));
    }
}
