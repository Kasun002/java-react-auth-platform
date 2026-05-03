package com.shop.auth.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates password complexity per NIST 800-63B and PCI-DSS Req 8.3.6.
 *
 * <p>All failing rules are collected and reported in a single message so the
 * caller knows exactly what to fix without round-tripping multiple times.</p>
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int     MIN_LENGTH   = 12;
    private static final int     MAX_LENGTH   = 128;
    private static final Pattern HAS_UPPER    = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER    = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT    = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL  = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?~`]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) {
            return true; // @NotBlank handles null/blank — this validator focuses on complexity only
        }

        List<String> violations = new ArrayList<>();

        if (value.length() < MIN_LENGTH) {
            violations.add("at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            violations.add("at most " + MAX_LENGTH + " characters");
        }
        if (!HAS_UPPER.matcher(value).find()) {
            violations.add("at least one uppercase letter (A-Z)");
        }
        if (!HAS_LOWER.matcher(value).find()) {
            violations.add("at least one lowercase letter (a-z)");
        }
        if (!HAS_DIGIT.matcher(value).find()) {
            violations.add("at least one digit (0-9)");
        }
        if (!HAS_SPECIAL.matcher(value).find()) {
            violations.add("at least one special character (!@#$%^&* etc.)");
        }

        if (violations.isEmpty()) {
            return true;
        }

        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(
                "Password must contain: " + String.join(", ", violations))
           .addConstraintViolation();
        return false;
    }
}
