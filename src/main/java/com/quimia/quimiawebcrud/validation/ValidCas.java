package com.quimia.quimiawebcrud.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * O texto é um número CAS com o dígito verificador certo. Vazio passa: para exigir o campo existe o @NotBlank.
 */
@Documented
@Constraint(validatedBy = ValidCas.Check.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCas {

    String message() default "O número CAS não é válido: confira os números (o dígito verificador não bate).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Check implements ConstraintValidator<ValidCas, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.isBlank() || CasNumber.isValid(value);
        }
    }
}
