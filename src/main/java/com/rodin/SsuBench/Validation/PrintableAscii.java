package com.rodin.SsuBench.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PrintableAsciiValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface PrintableAscii {
    String message() default "Поле должно содержать только печатаемые ASCII символы";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
