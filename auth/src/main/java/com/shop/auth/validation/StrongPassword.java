package com.shop.auth.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Enforces banking-grade password complexity (NIST 800-63B + PCI-DSS Req 8.3.6):
 * <ul>
 *   <li>12–128 characters</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one digit</li>
 *   <li>At least one special character</li>
 * </ul>
 *
 * <p>Null values pass — pair with {@code @NotBlank} to reject null/empty.</p>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password does not meet the required complexity rules";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
