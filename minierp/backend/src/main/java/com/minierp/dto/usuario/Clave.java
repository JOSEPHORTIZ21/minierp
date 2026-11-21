package com.minierp.dto.usuario;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

/**
 * Política de contraseñas: de 8 a 72 caracteres (72 es el límite de BCrypt), con al menos una letra y un número.
 */
@NotBlank
@Size(min = 8, max = 72)
@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Clave {

    String message() default "La contraseña debe tener entre 8 y 72 caracteres, con al menos una letra y un número";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
