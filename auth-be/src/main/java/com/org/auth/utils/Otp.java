package com.org.auth.utils;

import java.security.SecureRandom;

public final class Otp {

    private Otp() {}

    public static String generateRawOtp() {
        // Full 10^6 entropy (000000–999999). Zero-padded to always produce exactly 6 digits.
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}
