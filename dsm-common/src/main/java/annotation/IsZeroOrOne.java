package annotation;

import annotation.validator.IsZeroOrOneValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {IsZeroOrOneValidator.class}) // 指定校验器
public @interface IsZeroOrOne {
    String message() default "只能是0或1";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}