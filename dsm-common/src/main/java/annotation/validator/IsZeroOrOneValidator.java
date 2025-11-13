package annotation.validator;

import annotation.IsZeroOrOne;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsZeroOrOneValidator implements ConstraintValidator<IsZeroOrOne, Number> {
    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        // 允许为null（若不允许null，需额外加@NotNull）
        if (value == null) {
            return true;
        }
        // 校验值是否为0或1（将Number转换为int后进行比较）
        int intValue = value.intValue();
        return intValue == 0 || intValue == 1;
    }
}