package com.rodin.SsuBench.Validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PrintableAsciiValidator implements ConstraintValidator<PrintableAscii, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        for (char c : value.toCharArray()) {
            if (c > 127 || c < 32) {
                return false;
            }
        }
        return true;
    }
}
