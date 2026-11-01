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
 * O texto é um CNPJ com os dígitos verificadores certos. Vazio passa: para exigir o campo existe o @NotBlank.
 */
@Documented
@Constraint(validatedBy = ValidCnpj.Check.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCnpj {

    String message() default "O CNPJ não é válido: confira os números (os dígitos verificadores não batem).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Check implements ConstraintValidator<ValidCnpj, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.isBlank() || CnpjNumber.isValid(value);
        }
    }
}
